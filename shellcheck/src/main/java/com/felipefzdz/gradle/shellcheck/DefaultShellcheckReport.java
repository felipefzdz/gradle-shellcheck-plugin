package com.felipefzdz.gradle.shellcheck;

import javax.inject.Inject;

abstract class DefaultShellcheckReport implements ShellcheckReport {
    private final String name;

    @Inject
    public DefaultShellcheckReport(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
