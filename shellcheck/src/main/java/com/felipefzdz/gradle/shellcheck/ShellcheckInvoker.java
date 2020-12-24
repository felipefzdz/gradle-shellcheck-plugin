package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.internal.logging.ConsoleRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
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

    public static void invoke(Shellcheck task) throws IOException, InterruptedException, TransformerException, ParserConfigurationException, SAXException {
        final ShellcheckReports reports = task.getReports();
        File xmlDestination = reports.getXml().getDestination();

        if (isHtmlReportEnabledOnly(reports)) {
            xmlDestination = new File(task.getTemporaryDir(), reports.getXml().getDestination().getName());
        }

        if (task.isShowViolations()) {
            task.getLogger().lifecycle(runShellcheck(task.getShellScripts(), "tty", task.getLogger()));
        }

        if (reports.getXml().isEnabled() || reports.getHtml().isEnabled()) {
            String checkstyleFormatted = runShellcheck(task.getShellScripts(), "checkstyle", task.getLogger());
            Files.writeString(xmlDestination.toPath(), checkstyleFormatted);
        }

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

        if (isHtmlReportEnabledOnly(reports)) {
            Files.deleteIfExists(xmlDestination.toPath());
        }
        final ReportSummary reportSummary = calculateReportSummary(parseShellCheckXml(reports));
        if (reportSummary.filesWithError > 0) {
            final String message = getMessage(reports, reportSummary);
            if (task.getIgnoreFailures()) {
                task.getLogger().warn(message);
            } else {
                throw new GradleException(message);
            }
        }
    }

    private static Document parseShellCheckXml(ShellcheckReports reports) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(reports.getXml().getDestination());
    }

    public static String runShellcheck(File shellScripts, String format, Logger logger) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
            "docker",
            "run",
            "--rm",
            "-v",
            shellScripts.getAbsolutePath() + ":" + shellScripts.getAbsolutePath(),
            "koalaman/shellcheck-alpine:v0.7.1",
            "sh",
            "-c",
            "find " + shellScripts.getAbsolutePath() + " -name '*.sh' | xargs shellcheck -f " + format);

        ProcessBuilder builder = new ProcessBuilder(command)
            .directory(shellScripts)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true);
        builder.environment().clear();

        builder.redirectErrorStream(true);

        Process process = builder.start();
        process.info().commandLine().ifPresent(c -> logger.debug("Docker command to run Shellcheck: " + c));

        StringBuilder processOutput = new StringBuilder();

        try (BufferedReader processOutputReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));) {
            String readLine;
            while ((readLine = processOutputReader.readLine()) != null) {
                processOutput.append(readLine).append(System.lineSeparator());
            }
            process.waitFor();
        }
        return processOutput.toString().trim();
    }

    private static boolean isHtmlReportEnabledOnly(ShellcheckReports reports) {
        return !reports.getXml().isEnabled() && reports.getHtml().isEnabled();
    }

    private static String getMessage(ShellcheckReports reports, ReportSummary reportSummary) {
        return "Shellcheck violations were found." + getReportUrlMessage(reports) + "" + getViolationMessage(reportSummary);
    }

    private static ReportSummary calculateReportSummary(Document reportXml) {
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
        return new ReportSummary(filesWithError.size(), violationsBySeverityCount.size());
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
        private int filesWithError;
        private int violationsBySeverity;

        public ReportSummary(int filesWithError, int violationsBySeverity) {
            this.filesWithError = filesWithError;
            this.violationsBySeverity = violationsBySeverity;
        }
    }
}
