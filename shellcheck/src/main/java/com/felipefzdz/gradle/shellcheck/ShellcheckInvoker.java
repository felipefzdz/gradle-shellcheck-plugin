package com.felipefzdz.gradle.shellcheck;

import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.internal.logging.ConsoleRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ShellcheckInvoker {

    private static final String SHELLCHECK_NOFRAMES_SORTED_XSL = "shellcheck-noframes-sorted.xsl";

    public static void invoke(Shellcheck task) {
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
                maybeReport = Optional.of(runShellcheck(task, "tty"));

                FileUtils.writeStringToFile(txtDestination, maybeReport.get(), StandardCharsets.UTF_8);
            }
            if (task.isShowViolations()) {
                task.getLogger().lifecycle(maybeReport.orElse(runShellcheck(task, "tty")));
            }
        } catch (IOException | InterruptedException e) {
            throw new GradleException("Error while handling Shellcheck tty report", e);
        }
    }

    private static Optional<Document> handleCheckstyleReport(Shellcheck task, File xmlDestination) {
        try {
            final String rawOutput = runShellcheck(task, "checkstyle").trim();
            if (rawOutput.isEmpty() || rawOutput.contains("No files specified.")) {
                return Optional.empty();
            }
            assertContainsXml(rawOutput);
            String checkstyleFormatted = rawOutput.substring(rawOutput.indexOf("<?xml"));
            task.getLogger().debug("Shellcheck output: " + checkstyleFormatted);
            FileUtils.writeStringToFile(xmlDestination, checkstyleFormatted, StandardCharsets.UTF_8);
            return Optional.of(parseShellCheckXml(xmlDestination));
        } catch (IOException | InterruptedException | ParserConfigurationException | SAXException e) {
            throw new GradleException("Error while handling Shellcheck checkstyle report", e);
        }
    }

    private static File calculateReportDestination(Shellcheck task, SingleFileReport report) {
        return report.isEnabled() ? report.getDestination() : new File(task.getTemporaryDir(), report.getDestination().getName());
    }

    private static void assertContainsXml(String potentialXml) {
        if (!potentialXml.contains("<?xml version")) {
            throw new GradleException(String.format("Error while executing shellcheck: %s", potentialXml));
        }
    }

    private static Document parseShellCheckXml(File xmlDestination) throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlDestination);
    }

    private static String quoted(String txt) {
        return "\"" + txt + "\"";
    }

    public static String runShellcheck(Shellcheck task, String format) throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();

        final Set<File> sources = (Set<File>) task.getSources()
            .getFiles();

        if(sources.isEmpty()) {
            return "No source files specified.";
        }else{
            task.getLogger().warn("sources: " + sources.toString());
        }

        if(task.isUseDocker()) {
            command.add("docker");
            command.add("run");
            command.add("--rm");

            final List<String> volumes = sources.stream()
                .map(File::getAbsolutePath)
                .map(folder -> folder + ":" + folder)
                .collect(toList());
            volumes.forEach(volume -> {
                command.add("-v");
                command.add(volume);
            });
            command.add("koalaman/shellcheck-alpine:" + task.getShellcheckVersion());
        }
        String cmd = findCommand(sources.stream().map(File::getAbsolutePath).collect(joining(" "))) + " | xargs " + task.getShellcheckBinary() + " -f " + format + " --severity=" + task.getSeverity();
        command.add("sh");
        command.add("-c");
        command.add(cmd);

        task.getLogger().warn("Command to run Shellcheck: " + String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(task.getProjectDir())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true);
        builder.environment().clear();

        builder.redirectErrorStream(true);

        Process process = builder.start();

        StringBuilder processOutput = new StringBuilder();

        try (BufferedReader processOutputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String readLine;
            while ((readLine = processOutputReader.readLine()) != null) {
                processOutput.append(readLine).append(System.lineSeparator());
            }
            process.waitFor();
        }
        return processOutput.toString().trim();
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
