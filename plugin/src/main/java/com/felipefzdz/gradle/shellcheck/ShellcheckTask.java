package com.felipefzdz.gradle.shellcheck;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.plugins.quality.CheckstyleReports;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;

public class ShellcheckTask extends ConventionTask implements VerificationTask, Reporting<CheckstyleReports> {

    private int maxErrors;

    @TaskAction
    public void run() {
        ShellcheckInvoker.invoke(this);
    }

    /**
     * The maximum number of errors that are tolerated before breaking the build
     * or setting the failure property.
     *
     * @return the maximum number of errors allowed
     * @since 3.4
     */
    @Input
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

    @Override
    public CheckstyleReports getReports() {
        return null;
    }

    @Override
    public CheckstyleReports reports(Closure closure) {
        return null;
    }

    @Override
    public CheckstyleReports reports(Action<? super CheckstyleReports> configureAction) {
        return null;
    }

    @Override
    public void setIgnoreFailures(boolean ignoreFailures) {

    }

    @Override
    public boolean getIgnoreFailures() {
        return false;
    }
}
