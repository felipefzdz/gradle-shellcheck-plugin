package com.felipefzdz.gradle.shellcheck;

import javax.inject.Inject;

abstract class DefaultShellcheckHtmlReport extends DefaultShellcheckReport implements ShellcheckHtmlReport {
    @Inject
    public DefaultShellcheckHtmlReport(String name) {
        super(name);
    }
}
