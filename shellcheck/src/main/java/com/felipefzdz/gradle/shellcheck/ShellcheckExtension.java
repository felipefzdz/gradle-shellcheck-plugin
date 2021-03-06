package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.quality.CodeQualityExtension;

public class ShellcheckExtension extends CodeQualityExtension {

    private final Project project;

    private FileCollection sources;
    private boolean showViolations = true;
    private String shellcheckVersion = "v0.7.1";
    private String severity = "style";

    public ShellcheckExtension(Project project) {
        this.project = project;
    }

    public FileCollection getSources() {
        return sources;
    }

    public void setSources(FileCollection sources) {
        this.sources = sources;
    }

    /**
     * Whether rule violations are to be displayed on the console. Defaults to true.
     * <p>
     * Example: showViolations = false
     */
    public boolean isShowViolations() {
        return showViolations;
    }

    /**
     * Whether rule violations are to be displayed on the console. Defaults to true.
     * <p>
     * Example: showViolations = false
     */
    public void setShowViolations(boolean showViolations) {
        this.showViolations = showViolations;
    }

    public String getShellcheckVersion() {
        return shellcheckVersion;
    }

    public void setShellcheckVersion(String shellcheckVersion) {
        this.shellcheckVersion = shellcheckVersion;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
