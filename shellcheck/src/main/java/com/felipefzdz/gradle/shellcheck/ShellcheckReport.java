package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;

public interface ShellcheckReport extends Named  {

    @Input
    @Override
    String getName();

    @Input
    Property<Boolean> getRequired();

    /**
     * The location on the filesystem to generate the report to.
     */
    @OutputFile
    RegularFileProperty getOutputLocation();
}
