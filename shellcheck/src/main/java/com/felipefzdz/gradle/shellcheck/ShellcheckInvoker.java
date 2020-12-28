package com.felipefzdz.gradle.shellcheck;

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
import java.io.*;
import java.nio.file.Files;
import java.util.*;

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

                Files.writeString(txtDestination.toPath(), maybeReport.get());
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
            String checkstyleFormatted = runShellcheck(task, "checkstyle");
            if (checkstyleFormatted.contains("No files specified.")) {
                return Optional.empty();
            }
            assertValidXml(checkstyleFormatted);
            task.getLogger().debug("Shellcheck output: " + checkstyleFormatted);
            Files.writeString(xmlDestination.toPath(), checkstyleFormatted);
            return Optional.of(parseShellCheckXml(xmlDestination));
        } catch (IOException | InterruptedException | ParserConfigurationException | SAXException e) {
            throw new GradleException("Error while handling Shellcheck checkstyle report", e);
        }
    }

    private static File calculateReportDestination(Shellcheck task, SingleFileReport report) {
        return report.isEnabled() ? report.getDestination() : new File(task.getTemporaryDir(), report.getDestination().getName());
    }

    private static void assertValidXml(String potentialXml) {
        if (!potentialXml.startsWith("<?xml version")) {
            throw new GradleException(String.format("Error while executing shellcheck: %s", potentialXml));
        }
    }

    private static Document parseShellCheckXml(File xmlDestination) throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlDestination);
    }

    public static String runShellcheck(Shellcheck task, String format) throws IOException, InterruptedException {
        final File source = task.getSource();
        List<String> command = Arrays.asList(
            "docker",
            "run",
            "--rm",
            "-v",
            source.getAbsolutePath() + ":" + source.getAbsolutePath(),
            "koalaman/shellcheck-alpine:" + task.getShellcheckVersion(),
            "sh",
            "-c",
            findCommand(source) + " | xargs shellcheck -f " + format + " --severity=" + task.getSeverity());

        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(source)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true);
        builder.environment().clear();

        builder.redirectErrorStream(true);

        Process process = builder.start();
        process.info().commandLine().ifPresent(c -> task.getLogger().debug("Docker command to run Shellcheck: " + c));

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

    private static String findCommand(File source) {
        return "find " + source.getAbsolutePath() + " -type f \\( -name '*.sh' -o -name '*.bash' -o -name '*.ksh' -o -name '*.bashrc' -o -name '*.bash_profile' -o -name '*.bash_login' -o -name '*.bash_logout' \\)";
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
