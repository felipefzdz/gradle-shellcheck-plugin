package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reporting.ReportingExtension;

import java.io.File;
import java.util.concurrent.Callable;

public class ShellcheckPlugin implements Plugin<Project> {

    protected ShellcheckExtension extension;

    public void apply(Project project) {
        project.getPluginManager().apply(ReportingBasePlugin.class);
        extension = (ShellcheckExtension) project.getExtensions().create("shellcheck", ShellcheckExtension.class, project);
        project.getTasks().register("shellcheck", Shellcheck.class);
        project.getTasks().withType(Shellcheck.class).configureEach(task -> configureTask((Shellcheck) task, project));
    }

    private void configureTask(Shellcheck task, Project project) {
        configureTaskConventionMapping(task, project);
        configureReportsConventionMapping(task, project);
    }

    private void configureTaskConventionMapping(Shellcheck task, Project project) {
        ConventionMapping taskMapping = task.getConventionMapping();
        taskMapping.map("sources", (Callable<FileCollection>) () -> extension.getSources());
        taskMapping.map("sourceFiles", (Callable<FileCollection>) () -> extension.getSourceFiles());
        taskMapping.map("ignoreFailures", (Callable<Boolean>) () -> extension.isIgnoreFailures());
        taskMapping.map("showViolations", (Callable<Boolean>) () -> extension.isShowViolations());
        taskMapping.map("useDocker", (Callable<Boolean>) () -> extension.isUseDocker());
        taskMapping.map("shellcheckVersion", (Callable<String>) () -> extension.getShellcheckVersion());
        taskMapping.map("severity", (Callable<String>) () -> extension.getSeverity());
        taskMapping.map("shellcheckBinary", (Callable<String>) () -> extension.getShellcheckBinary());
        taskMapping.map("installer", (Callable<String>) () -> extension.getInstaller());
        taskMapping.map("workingDir", (Callable<File>) () -> extension.getWorkingDir());
        taskMapping.map("additionalArguments", (Callable<String>) () -> extension.getAdditionalArguments());
        final ConventionMapping extensionMapping = conventionMappingOf(extension);
        extensionMapping.map("reportsDir", (Callable<File>) () -> project.getExtensions().getByType(ReportingExtension.class).file("shellcheck"));
    }

    private void configureReportsConventionMapping(Shellcheck task, Project project) {
        ProjectLayout layout = project.getLayout();
        ProviderFactory providers = project.getProviders();
        Provider<Directory> reportsDir = layout.dir(providers.provider(() -> extension.getReportsDir()));
        task.getReports().getAll()
                .forEach(report -> {
                    report.getRequired().convention(true);
                    String reportFileName = "shellcheck." + report.getName();
                    report.getOutputLocation().convention(reportsDir.map(dir -> dir.file(reportFileName)));
                });
    }

    protected static ConventionMapping conventionMappingOf(Object object) {
        return ((IConventionAware) object).getConventionMapping();
    }
}
