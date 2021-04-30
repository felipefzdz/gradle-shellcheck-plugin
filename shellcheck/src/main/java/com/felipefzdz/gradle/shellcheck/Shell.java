package com.felipefzdz.gradle.shellcheck;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class Shell {
    static String run(String command, File projectDir) throws IOException, InterruptedException {
        return run(asList(command.split("\\s+")), projectDir);
    }
    static String run(List<String> command, File projectDir) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(projectDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true);
        builder.environment().clear();

        builder.redirectErrorStream(true);

        Process process = builder.start();

        StringBuilder processOutput = new StringBuilder();

        try (BufferedReader processOutputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String readLine;
            while ((readLine = processOutputReader.readLine()) != null) {
                processOutput.append(readLine).append(System.lineSeparator());
            }
            process.waitFor();
        }
        return processOutput.toString().trim();
    }
}
