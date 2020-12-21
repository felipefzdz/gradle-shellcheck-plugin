package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.reporting.CustomizableHtmlReport;
import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.Internal;

/**
 * The reporting configuration for the {@link Shellcheck} task.
 */
public interface ShellcheckReports extends ReportContainer<SingleFileReport> {
    /**
     * The shellcheck HTML report.
     * <p>
     * This report IS enabled by default.
     * <p>
     * Enabling this report will also cause the XML report to be generated, as the HTML is derived from the XML.
     *
     * @return The shellcheck HTML report
     */
    @Internal
    CustomizableHtmlReport getHtml();

    /**
     * The shellcheck XML report
     * <p>
     * This report IS enabled by default.
     *
     * @return The shellcheck XML report
     */
    @Internal
    SingleFileReport getXml();
}
