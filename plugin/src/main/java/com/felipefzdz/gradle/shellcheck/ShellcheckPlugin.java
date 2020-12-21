package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.ConventionMapping;

import java.io.File;
import java.util.concurrent.Callable;

public class ShellcheckPlugin implements Plugin<Project> {

    protected ShellcheckExtension extension;

    public void apply(Project project) {
        extension = project.getExtensions().create("shellcheck", ShellcheckExtension.class, project);
        project.getTasks().register("shellcheck", ShellcheckTask.class);
        project.getTasks().withType(ShellcheckTask.class).configureEach(this::configureTaskConventionMapping);
    }

    private void configureTaskConventionMapping(ShellcheckTask task) {
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("shellScripts", (Callable<File>) () -> extension.getShellScripts());
    }
}
