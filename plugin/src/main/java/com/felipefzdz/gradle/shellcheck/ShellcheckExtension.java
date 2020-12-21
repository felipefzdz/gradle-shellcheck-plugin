package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CodeQualityExtension;

import java.io.File;

public class ShellcheckExtension extends CodeQualityExtension {

    private final Project project;

    private File shellScripts;
    private boolean showViolations = true;
    private String excludeError;


    public ShellcheckExtension(Project project) {
        this.project = project;
    }

    public File getShellScripts() {
        return shellScripts;
    }

    public void setShellScripts(File shellScripts) {
        this.shellScripts = shellScripts;
    }

    /**
     * Whether rule violations are to be displayed on the console. Defaults to <tt>true</tt>.
     *
     * Example: showViolations = false
     */
    public boolean isShowViolations() {
        return showViolations;
    }

    /**
     * Whether rule violations are to be displayed on the console. Defaults to <tt>true</tt>.
     *
     * Example: showViolations = false
     */
    public void setShowViolations(boolean showViolations) {
        this.showViolations = showViolations;
    }

    public String getExcludeError() {
        return excludeError;
    }

    public void setExcludeError(String excludeError) {
        this.excludeError = excludeError;
    }
}
