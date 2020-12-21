package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CodeQualityExtension;

import java.io.File;

public class ShellcheckExtension extends CodeQualityExtension {

    private final Project project;

    private File shellScripts;

    public ShellcheckExtension(Project project) {
        this.project = project;
    }

    public File getShellScripts() {
        return shellScripts;
    }

    public void setShellScripts(File shellScripts) {
        this.shellScripts = shellScripts;
    }
}
