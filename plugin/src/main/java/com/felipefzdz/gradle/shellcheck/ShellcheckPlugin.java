package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ShellcheckPlugin implements Plugin<Project> {

    public void apply(Project project) {
        // Register a task
        project.getTasks().register("shellcheck", task -> {
            task.doLast(s -> System.out.println("Hello from plugin 'com.felipefzdz.gradle.shellcheck'"));
        });
    }
}
