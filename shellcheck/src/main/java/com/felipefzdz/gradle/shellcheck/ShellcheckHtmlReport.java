package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import javax.annotation.Nullable;

public interface ShellcheckHtmlReport extends ShellcheckReport {

    /**
     * The stylesheet to use to generate the HTML report.
     *
     * @return the stylesheet to use to generate the HTML report
     */
    @Nullable
    @Optional
    @Nested
    TextResource getStylesheet();

    /**
     * The stylesheet to use to generate the report.
     *
     * @param stylesheet the stylesheet to use to generate the HTML report
     */
    void setStylesheet(@Nullable TextResource stylesheet);
}
