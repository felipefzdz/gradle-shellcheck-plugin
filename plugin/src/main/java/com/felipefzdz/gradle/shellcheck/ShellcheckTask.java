package com.felipefzdz.gradle.shellcheck;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

public class ShellcheckTask extends ConventionTask implements VerificationTask {

    private File shellScripts;

    @TaskAction
    public void run() throws IOException, InterruptedException {
        ShellcheckInvoker.invoke(this);
    }

    @Override
    public void setIgnoreFailures(boolean ignoreFailures) {
    }

    @Override
    public boolean getIgnoreFailures() {
        return false;
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public File getShellScripts() {
        return shellScripts;
    }

    public void setShellScripts(File shellScripts) {
        this.shellScripts = shellScripts;
    }
}
