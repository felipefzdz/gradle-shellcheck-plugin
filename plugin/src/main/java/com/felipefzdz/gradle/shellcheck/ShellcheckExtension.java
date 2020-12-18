package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CodeQualityExtension;

public class ShellcheckExtension extends CodeQualityExtension {

    private final Project project;

    public ShellcheckExtension(Project project) {
        this.project = project;
    }

    private int maxErrors;

    private String fileToCheck;

    /**
     * The maximum number of errors that are tolerated before breaking the build
     * or setting the failure property. Defaults to <tt>0</tt>.
     * <p>
     * Example: maxErrors = 42
     *
     * @return the maximum number of errors allowed
     * @since 3.4
     */
    public int getMaxErrors() {
        return maxErrors;
    }

    /**
     * Set the maximum number of errors that are tolerated before breaking the build.
     *
     * @param maxErrors number of errors allowed
     * @since 3.4
     */
    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

    public String getFileToCheck() {
        return fileToCheck;
    }

    public void setFileToCheck(String fileToCheck) {
        this.fileToCheck = fileToCheck;
    }
}
