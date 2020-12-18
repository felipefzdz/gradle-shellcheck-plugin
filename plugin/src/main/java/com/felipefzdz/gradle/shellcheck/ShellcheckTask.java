package com.felipefzdz.gradle.shellcheck;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.plugins.quality.CheckstyleReports;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;

public class ShellcheckTask extends ConventionTask implements VerificationTask, Reporting<CheckstyleReports> {

    @TaskAction
    public void run() {
        ShellcheckInvoker.invoke(this);
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
