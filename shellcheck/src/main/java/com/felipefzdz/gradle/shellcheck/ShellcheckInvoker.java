package com.felipefzdz.gradle.shellcheck;

import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.internal.logging.ConsoleRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.felipefzdz.gradle.shellcheck.Shell.run;
import static java.util.stream.Collectors.joining;

public class ShellcheckInvoker {

    private static final String SHELLCHECK_NOFRAMES_SORTED_XSL = "shellcheck-noframes-sorted.xsl";

    public static void invoke(Shellcheck task) {
        maybeInstallShellcheck(task);
        final ShellcheckReports reports = task.getReports();
        final File xmlDestination = calculateReportDestination(task, reports.getXml());

        handleCheckstyleReport(task, xmlDestination).ifPresent(parsedReport -> {
            handleTtyReport(task, reports, calculateReportDestination(task, reports.getTxt()));
            handleHtmlReport(reports, xmlDestination);
            calculateReportSummary(parsedReport).ifPresent(reportSummary -> {
                final String message = getMessage(reports, reportSummary);
                if (task.getIgnoreFailures()) {
                    task.getLogger().warn(message);
                } else {
                    throw new GradleException(message);
                }
            });
        });

    }

    private static void maybeInstallShellcheck(Shellcheck task) {
        try {
            if (!task.isUseDocker()) {
                ShellcheckInstaller.maybeInstallShellcheck(task.getInstaller(), task.getWorkingDir(), task.getLogger());
            }
        } catch (IOException | InterruptedException e) {
            throw new GradleException("Error installing Shellcheck ", e);
        }
    }

    private static void handleHtmlReport(ShellcheckReports reports, File xmlDestination) {
        try {
            if (reports.getHtml().isEnabled()) {
                TransformerFactory factory = TransformerFactory.newInstance();
                InputStream stylesheet = reports.getHtml().getStylesheet() != null ?
                        new FileInputStream(reports.getHtml().getStylesheet().asFile()) :
                        ShellcheckInvoker.class.getClassLoader().getResourceAsStream(SHELLCHECK_NOFRAMES_SORTED_XSL);
                Source xslt = new StreamSource(stylesheet);
                Transformer transformer = factory.newTransformer(xslt);

                Source text = new StreamSource(xmlDestination);
                transformer.transform(text, new StreamResult(reports.getHtml().getDestination()));
            }
            if (!reports.getXml().isEnabled()) {
                Files.deleteIfExists(xmlDestination.toPath());
            }
        } catch (TransformerException | IOException e) {
            throw new GradleException("Error while handling Shellcheck html report", e);
        }
    }

    private static void handleTtyReport(Shellcheck task, ShellcheckReports reports, File txtDestination) {
        try {
            Optional<String> maybeReport = Optional.empty();
            if (reports.getTxt().isEnabled()) {
                maybeReport = Optional.of(runShellcheck(task, "tty")).map(l -> String.join("\n\n", l));

                FileUtils.writeStringToFile(txtDestination, maybeReport.get(), StandardCharsets.UTF_8);
            }
            if (task.isShowViolations()) {
                task.getLogger().lifecycle(maybeReport.orElse(String.join("\n\n", runShellcheck(task, "tty")).trim()));
            }
        } catch (IOException | InterruptedException e) {
            throw new GradleException("Error while handling Shellcheck tty report", e);
        }
    }

    private static Optional<Document> handleCheckstyleReport(Shellcheck task, File xmlDestination) {
        try {
            final List<String> output = runShellcheck(task, "checkstyle");
            task.getLogger().debug("Shellcheck output: " + output);

            Document mergedCheckstyleXmlReport = mergeCheckstyleXml(output);

            if(!mergedCheckstyleXmlReport.getDocumentElement().hasChildNodes()) {
                return Optional.empty();
            }
            writeXmlToFile(mergedCheckstyleXmlReport, xmlDestination);
            return Optional.of(mergedCheckstyleXmlReport);
        } catch (IOException | InterruptedException | ParserConfigurationException | SAXException | TransformerException e) {
            throw new GradleException("Error while handling Shellcheck checkstyle report", e);
        }
    }

    private static Document mergeCheckstyleXml(List<String> output) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document merged = documentBuilder.parse(new InputSource(new StringReader("<?xml version='1.0' encoding='UTF-8'?><checkstyle version='4.3'></checkstyle>")));

        for(String rawOutput: output) {
            if (rawOutput.isEmpty() || rawOutput.contains("No files specified.")) {
                continue;
            }
            assertContainsXml(rawOutput);
            String checkstyleFormatted = rawOutput.substring(rawOutput.indexOf("<?xml"));

            Document report = documentBuilder.parse(new InputSource(new StringReader(checkstyleFormatted)));
            NodeList childNodes = report.getDocumentElement().getChildNodes();
            for(int i = 0; i < childNodes.getLength(); i++) {
                Node importedNode = merged.importNode(childNodes.item(i), true);
                merged.getDocumentElement().appendChild(importedNode);
            }
        }
        return merged;
    }

    private static void writeXmlToFile(Document merged, File xmlDestination) throws IOException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(merged);
        FileWriter writer = new FileWriter(xmlDestination);
        StreamResult streamResult = new StreamResult(writer);
        transformer.transform(source, streamResult);
    }

    private static File calculateReportDestination(Shellcheck task, SingleFileReport report) {
        return report.isEnabled() ? report.getDestination() : new File(task.getTemporaryDir(), report.getDestination().getName());
    }

    private static void assertContainsXml(String potentialXml) {
        if (!potentialXml.contains("<?xml version")) {
            throw new GradleException(String.format("Error while executing shellcheck: %s", potentialXml));
        }
    }

    private static String quoted(String txt) {
        return "\"" + txt + "\"";
    }

    public static List<String> runShellcheck(Shellcheck task, String format) throws IOException, InterruptedException {
        if (isNullOrEmpty(task.getSources()) && isNullOrEmpty(task.getSourceFiles())) {
            return Collections.singletonList("No files specified.");
        } else {
            task.getLogger().debug("source dirs: " + task.getSources());
            task.getLogger().debug("source files: " + task.getSourceFiles());
        }

        List<String> shellcheckOutput = new ArrayList<>();
        if (!isNullOrEmpty(task.getSources())) {
            shellcheckOutput.addAll(runShellcheckOnDirs(task, format, task.getSources().getFiles()));
        }
        if (!isNullOrEmpty(task.getSourceFiles())) {
            shellcheckOutput.addAll(runShellcheckOnFiles(task, format, task.getSourceFiles().getFiles()));
        }
        return shellcheckOutput;
    }

    private static boolean isNullOrEmpty(FileCollection collection) {
        return collection == null || collection.isEmpty();
    }

    private static List<String> runShellcheckOnDirs(Shellcheck task, String format, Set<File> sourceDirs) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        maybePrepareCommandToUseDocker(command, task.getWorkingDir(), task.getShellcheckVersion(), task.isUseDocker());
        final String shellcheckBinary = task.isUseDocker() ? "shellcheck" : task.getShellcheckBinary();

        StringJoiner joiner = new StringJoiner(" ");
        for (File sourceDir : sourceDirs) {
            String canonicalPath = quoted(sourceDir.getCanonicalPath());
            joiner.add(canonicalPath);
        }

        String cmd = findCommand(joiner.toString()) + " | xargs " + shellcheckBinary + " -f " + format + " --severity=" + task.getSeverity() + " " + task.getAdditionalArguments();
        command.add("sh");
        command.add("-c");
        command.add(cmd);

        task.getLogger().debug("Command to run Shellcheck: " + String.join(" ", command));

        return Collections.singletonList(run(command, task.getWorkingDir(), task.getLogger()).trim());
    }

    private static List<String> runShellcheckOnFiles(Shellcheck task, String format, Set<File> sourceFiles) {
        final List<String> command = new ArrayList<>();
        maybePrepareCommandToUseDocker(command, task.getWorkingDir(), task.getShellcheckVersion(), task.isUseDocker());
        final String shellcheckBinary = task.isUseDocker() ? "shellcheck" : task.getShellcheckBinary();
        String cmd = shellcheckBinary + " -f " + format + " --severity=" + task.getSeverity() + " " + task.getAdditionalArguments();
        command.add("sh");
        command.add("-c");

        return sourceFiles.stream().map(f -> {
            try {
                List<String> fileCommand = new ArrayList<>(command);
                fileCommand.add(cmd + " " + quoted(f.getCanonicalPath()));

                task.getLogger().debug("Command to run Shellcheck: " + String.join(" ", fileCommand));

                return run(fileCommand, task.getWorkingDir(), task.getLogger()).trim();
            } catch (IOException | InterruptedException e) {
                throw new GradleException(e.getMessage(), e);
            }
        }).collect(Collectors.toList());
    }

    private static void maybePrepareCommandToUseDocker(List<String> command, File workingDir, String shellcheckVersion, boolean useDocker) {
        if (useDocker) {
            command.add("docker");
            command.add("run");
            command.add("--rm");

            command.add("-v");
            command.add(workingDir.getAbsolutePath() + ":" + workingDir.getAbsolutePath());
            command.add("-w");
            command.add(workingDir.getAbsolutePath());
            command.add("koalaman/shellcheck-alpine:" + shellcheckVersion);
        }
    }

    private static String findCommand(String sources) {
        return "/usr/bin/find " + sources + " -type f \\( -name '*.sh' -o -name '*.bash' -o -name '*.ksh' -o -name '*.bashrc' -o -name '*.bash_profile' -o -name '*.bash_login' -o -name '*.bash_logout' \\)";
    }

    private static String getMessage(ShellcheckReports reports, ReportSummary reportSummary) {
        return "Shellcheck violations were found." + getReportUrlMessage(reports) + "" + getViolationMessage(reportSummary);
    }

    private static Optional<ReportSummary> calculateReportSummary(Document reportXml) {
        NodeList fileNodes = reportXml.getDocumentElement().getChildNodes();
        Set<String> filesWithError = new HashSet<>();
        Set<String> violationsBySeverityCount = new HashSet<>();
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Node fileNode = fileNodes.item(i);
            if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
                final NodeList errorNodes = fileNode.getChildNodes();
                for (int j = 0; j < errorNodes.getLength(); j++) {
                    Node errorNode = errorNodes.item(j);
                    if (errorNode.getNodeType() == Node.ELEMENT_NODE) {
                        if ("error".equals(errorNode.getNodeName())) {
                            filesWithError.add(fileNode.getAttributes().getNamedItem("name").getNodeValue());
                            violationsBySeverityCount.add(errorNode.getAttributes().getNamedItem("severity").getNodeValue());
                        }
                    }
                }
            }
        }
        return filesWithError.size() > 0 ? Optional.of(new ReportSummary(filesWithError.size(), violationsBySeverityCount.size())) : Optional.empty();
    }

    private static String getReportUrlMessage(ShellcheckReports reports) {
        SingleFileReport report = reports.getHtml().isEnabled() ? reports.getHtml() : reports.getXml().isEnabled() ? reports.getXml() : null;
        return report != null ? " See the report at: " + new ConsoleRenderer().asClickableFileUrl(report.getDestination()) + "\n" : "\n";
    }

    private static String getViolationMessage(ReportSummary reportSummary) {
        return reportSummary.filesWithError > 0 ?
                "Shellcheck files with violations: " + reportSummary.filesWithError + "\nShellcheck violations by severity: " + reportSummary.violationsBySeverity :
                "\n";
    }

    static class ReportSummary {
        private final int filesWithError;
        private final int violationsBySeverity;

        public ReportSummary(int filesWithError, int violationsBySeverity) {
            this.filesWithError = filesWithError;
            this.violationsBySeverity = violationsBySeverity;
        }
    }
}
