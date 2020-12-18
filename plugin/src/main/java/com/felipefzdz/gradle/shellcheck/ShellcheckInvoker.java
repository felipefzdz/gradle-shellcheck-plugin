package com.felipefzdz.gradle.shellcheck;

public class ShellcheckInvoker {
    public static void invoke(ShellcheckTask task) {
        System.out.println("Hello from " + task.getMaxErrors());
    }
}
