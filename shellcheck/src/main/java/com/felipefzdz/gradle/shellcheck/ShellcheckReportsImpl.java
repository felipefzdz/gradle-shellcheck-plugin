package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShellcheckReportsImpl implements ShellcheckReports {

    private final ShellcheckHtmlReport htmlReport;
    private final ShellcheckReport xmlReport;
    private final ShellcheckReport txtReport;

    @Inject
    public ShellcheckReportsImpl(ObjectFactory objects) {
        this.htmlReport = objects.newInstance(DefaultShellcheckHtmlReport.class, "html");
        this.xmlReport = objects.newInstance(DefaultShellcheckReport.class, "xml");
        this.txtReport = objects.newInstance(DefaultShellcheckReport.class, "txt");
    }

    @Override
    public ShellcheckHtmlReport getHtml() {
        return htmlReport;
    }

    @Override
    public ShellcheckReport getXml() {
        return xmlReport;
    }

    @Override
    public ShellcheckReport getTxt() {
        return txtReport;
    }

    @Override
    public Collection<ShellcheckReport> getAll() {
        List<ShellcheckReport> shellcheckReports = new java.util.ArrayList<>();
        shellcheckReports.add(htmlReport);
        shellcheckReports.add(xmlReport);
        shellcheckReports.add(txtReport);
        return shellcheckReports;
    }

    @Nested
    public Collection<ShellcheckReport> getEnabledReports() {
        return Stream.of(htmlReport, xmlReport, txtReport)
                .filter(it -> it.getRequired().get())
                .collect(Collectors.toList());
    }
}
