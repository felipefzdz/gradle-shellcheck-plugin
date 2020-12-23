package com.felipefzdz.gradle.shellcheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
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

        final Optional<List<String>> maybeExcludeErrors = Optional.ofNullable(task.getExcludeErrors());
        if (task.isShowViolations()) {
            task.getLogger().lifecycle(runShellcheckWithJavaLibrary(task.getShellScripts(), "tty", maybeExcludeErrors, task.getLogger()));
        }

        if (reports.getXml().isEnabled() || reports.getHtml().isEnabled()) {
            String checkstyleFormatted = runShellcheckWithJavaLibrary(task.getShellScripts(), "checkstyle", maybeExcludeErrors, task.getLogger());
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

    public static String runShellcheckWithJavaLibrary(File shellScripts, String format, Optional<List<String>> maybeExcludeErrors, Logger logger) throws IOException, InterruptedException {
        final DockerClient dockerClient = getDockerClient();
        final String id = createContainer(shellScripts, maybeExcludeErrors, dockerClient, format);
        return startContainer(logger, dockerClient, id);
    }

    private static String startContainer(Logger logger, DockerClient dockerClient, String id) throws InterruptedException {
        try {
            final List<String> logs = new ArrayList<>();
            dockerClient.startContainerCmd(id).exec();
            final ResultCallback.Adapter<Frame> loggingCallback = new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame item) {
                    logs.add(new String(item.getPayload()));
                }
            };
            dockerClient
                .logContainerCmd(id)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(loggingCallback)
                .awaitStarted();
            loggingCallback.awaitCompletion();
            return String.join("\n", logs);
        } finally {
            try {
                dockerClient.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (NotFoundException | InternalServerErrorException e) {
                logger.error("Swallowed exception while removing container", e);
            }
        }
    }

    private static String createContainer(File shellScripts, Optional<List<String>> maybeExcludeErrors, DockerClient dockerClient, String format) {
        final CreateContainerCmd command = dockerClient.createContainerCmd("koalaman/shellcheck-alpine:v0.7.1");
        maybeExcludeErrors
            .map(exclusion -> "\"-e " + String.join(" -e ", exclusion) + "\"")
            .ifPresent(opts -> command.withEnv("SHELLCHECK_OPTS=\"" + opts + "\""));
        command
            .withHostConfig(HostConfig.newHostConfig().withBinds(Bind.parse(shellScripts.getAbsolutePath() + ":" + shellScripts.getAbsolutePath())))
            .withCmd("sh", "-c", "find " + shellScripts.getAbsolutePath() + " -name '*.sh' | xargs shellcheck -f " + format)
            .withAttachStdout(true);
        return command.exec().getId();
    }

    private static DockerClient getDockerClient() {
        final DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        final DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
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
        private final int filesWithError;
        private final int violationsBySeverity;

        public ReportSummary(int filesWithError, int violationsBySeverity) {
            this.filesWithError = filesWithError;
            this.violationsBySeverity = violationsBySeverity;
        }
    }
}
