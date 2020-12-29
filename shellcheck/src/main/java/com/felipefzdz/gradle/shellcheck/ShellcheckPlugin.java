package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reporting.ReportingExtension;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.gradle.api.internal.lambdas.SerializableLambdas.action;

public class ShellcheckPlugin implements Plugin<Project> {

    protected ShellcheckExtension extension;

    public void apply(Project project) {
        project.getPluginManager().apply(ReportingBasePlugin.class);
        extension = project.getExtensions().create("shellcheck", ShellcheckExtension.class, project);
        project.getTasks().register("shellcheck", Shellcheck.class);
        project.getTasks().withType(Shellcheck.class).configureEach(task -> configureTask(task, project));
    }

    private void configureTask(Shellcheck task, Project project) {
        configureTaskConventionMapping(task, project);
        configureReportsConventionMapping(task, project);
    }

    private void configureTaskConventionMapping(Shellcheck task, Project project) {
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("scripts", (Callable<ConfigurableFileCollection>) () -> filterShellScripts(project));
        taskMapping.map("sources", (Callable<List<File>>) () -> extension.getSources());
        taskMapping.map("ignoreFailures", (Callable<Boolean>) () -> extension.isIgnoreFailures());
        taskMapping.map("showViolations", (Callable<Boolean>) () -> extension.isShowViolations());
        taskMapping.map("shellcheckVersion", (Callable<String>) () -> extension.getShellcheckVersion());
        taskMapping.map("severity", (Callable<String>) () -> extension.getSeverity());
        final ConventionMapping extensionMapping = conventionMappingOf(extension);
        extensionMapping.map("reportsDir", (Callable<File>) () -> project.getExtensions().getByType(ReportingExtension.class).file("shellcheck"));
    }

    private ConfigurableFileCollection filterShellScripts(Project project) {
        final List<String> shellExtensions = Stream.of("sh", "bash", "ksh", "bashrc", "bash_profile", "bash_login", "bash_logout").map(extension -> String.format("**/*.%s", extension)).collect(toList());
        return project.files(project.files(extension.getSources()).getAsFileTree().matching(pattern -> pattern.include(shellExtensions)).getFiles());
    }

    private void configureReportsConventionMapping(Shellcheck task, Project project) {
        ProjectLayout layout = project.getLayout();
        ProviderFactory providers = project.getProviders();
        Provider<RegularFile> reportsDir = layout.file(providers.provider(() -> extension.getReportsDir()));
        task.getReports().all(action(report -> {
            report.getRequired().convention(true);
            report.getOutputLocation().convention(
                layout.getProjectDirectory().file(providers.provider(() -> {
                    String reportFileName = "shellcheck." + report.getName();
                    return new File(reportsDir.get().getAsFile(), reportFileName).getAbsolutePath();
                }))
            );
        }));
    }

    protected static ConventionMapping conventionMappingOf(Object object) {
        return ((IConventionAware) object).getConventionMapping();
    }
}
