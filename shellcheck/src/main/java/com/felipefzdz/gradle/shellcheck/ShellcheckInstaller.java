package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.felipefzdz.gradle.shellcheck.Shell.run;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ShellcheckInstaller {

    private static final Map<String, List<String>> INSTALLER_COMMANDS = new HashMap<String, List<String>>() {{
        put("cabal", asList("cabal update", "cabal install ShellCheck"));
        put("stack", asList("stack update", "stack install ShellCheck"));
        put("apt-get", singletonList("apt-get install shellcheck"));
        put("pacman", singletonList("pacman -S shellcheck"));
        put("emerge", singletonList("emerge --ask shellcheck"));
        put("yum", asList("yum -y install epel-release", "yum install ShellCheck"));
        put("dnf", singletonList("dnf install ShellCheck"));
        put("pkg", singletonList("pkg install hs-ShellCheck"));
        put("brew", singletonList("brew install shellcheck"));
        put("port", singletonList("sudo port install shellcheck"));
        put("pkg_add", singletonList("pkg_add shellcheck"));
        put("zypper", singletonList("zypper in ShellCheck"));
        put("eopkg", singletonList("eopkg install shellcheck"));
        put("conda", singletonList("conda install -c conda-forge shellcheck"));
        put("snap", singletonList("snap install --channel=edge shellcheck"));
        put("nix-env", singletonList("nix-env -iA nixpkgs.shellcheck"));
    }};

    public static void maybeInstallShellcheck(String installer, File projectDir, Logger logger) throws IOException, InterruptedException {
        if (installer.isEmpty()) {
            return;
        }

        if ("brew".equals(installer)) {
            final String commandShellcheck = run("command shellcheck", projectDir, logger);
            logger.debug("command shellcheck returned: " + commandShellcheck);
            if (!commandShellcheck.contains("shellcheck: command not found")) {
                logger.debug("Shellcheck is already installed. Skipping installation.");
                return;
            }
        }

        List<String> installerCommands = Optional.ofNullable(INSTALLER_COMMANDS.get(installer))
                .orElseThrow(() -> new IllegalArgumentException("Installer " + installer + " is not supported"));
        for (String installerCommand : installerCommands) {
            run(installerCommand, projectDir, logger);
        }
    }
}
