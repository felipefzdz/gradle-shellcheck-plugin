package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.quality.CodeQualityExtension;
import org.gradle.api.provider.Property;

abstract public class ShellcheckExtension extends CodeQualityExtension {

    abstract public Property<FileCollection> getSources();
    abstract public Property<Boolean> getContinueBuildOnFailure();
    abstract public Property<Boolean> getShowViolations();
    abstract public Property<Boolean> getUseDocker();
    abstract public Property<String> getSeverity();
    abstract public Property<String> getShellcheckImage();
    abstract public Property<String> getShellcheckVersion();
    abstract public Property<String> getShellcheckBinary();

    public ShellcheckExtension() {
        super();
        this.getUseDocker().convention(true);
        this.getShowViolations().convention(true);
        this.getContinueBuildOnFailure().convention(false);
        this.getShellcheckBinary().convention("shellcheck");
        this.getShellcheckImage().convention("koalaman/shellcheck-alpine");
        this.getShellcheckVersion().convention("v0.7.1");
        this.getSeverity().convention("style");
    }
}
