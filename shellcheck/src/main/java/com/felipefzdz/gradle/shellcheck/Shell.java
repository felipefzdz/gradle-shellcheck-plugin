package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class Shell {
    static String run(String command, File projectDir, Logger logger) throws IOException, InterruptedException {
        return run(asList(command.split("\\s+")), projectDir, logger);
    }
    static String run(List<String> command, File workingDir, Logger logger) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectErrorStream(true);
        prepareEnvironment(logger, builder.environment());

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

    private static void prepareEnvironment(Logger logger, final Map<String, String> environment) {
        final String path = environment.get("PATH");
        final String home = environment.get("HOME");
        environment.clear();
        environment.put("PATH", path);
        environment.put("HOME", home);
        logger.debug("Environment keys after preparing it for isolated Shellcheck execution: " + environment.keySet());
    }
}
