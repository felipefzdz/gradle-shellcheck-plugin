package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

class ShellcheckCrossVersionTest extends BaseInfraTest {

    boolean useDocker = true
    String shellcheckBinary = ""

    def "works on Gradle #gradleVersion"() {
        given:
        if (GradleVersion.version(gradleVersion) < GradleVersion.version("9.0")) {
            def javaHome = findCompatibleJavaHome()
            if (javaHome != null) {
                new File(testProjectDir.root, 'gradle.properties') << "org.gradle.java.home=${javaHome}\n"
            }
        }

        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
}
"""

        expect:
        // Cannot use withDebug(true) here: debug mode runs in-process (same JVM),
        // which ignores org.gradle.java.home and forces old Gradle on Java 25.
        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments("shellcheck", "--stacktrace")
                .withProjectDir(testProjectDir.root)
                .withGradleVersion(gradleVersion)
                .build()

        where:
        gradleVersion << ["7.0", "7.6.3", "8.10.1", "9.0", "9.4.0"]
    }

    private static String findCompatibleJavaHome() {
        // Older Gradle versions (< 9) don't support Java 25+
        for (String envVar in ['JDK11']) {
            def home = System.getenv(envVar)
            if (home != null && new File(home).directory) {
                return home
            }
        }
        try {
            def proc = ["/usr/libexec/java_home", "-v", "11"].execute()
            proc.waitFor()
            if (proc.exitValue() == 0) {
                return proc.text.trim()
            }
        } catch (ignored) {}
        return null
    }
}
