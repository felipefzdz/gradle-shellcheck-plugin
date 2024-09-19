package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.tasks.Internal;

import java.util.Collection;

/**
 * The reporting configuration for the {@link Shellcheck} task.
 */
public interface ShellcheckReports {
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
    ShellcheckHtmlReport getHtml();

    /**
     * The shellcheck XML report
     * <p>
     * This report IS enabled by default.
     *
     * @return The shellcheck XML report
     */
    @Internal
    ShellcheckReport getXml();

    /**
     * The shellcheck TTY report
     * This report IS enabled by default.
     *
     * @return The shellcheck TTY report
     */
    @Internal
    ShellcheckReport getTxt();

    @Internal
    Collection<ShellcheckReport> getAll();
}
