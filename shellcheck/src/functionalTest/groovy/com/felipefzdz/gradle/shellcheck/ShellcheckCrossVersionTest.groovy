package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

class ShellcheckCrossVersionTest extends BaseInfraTest {

    boolean useDocker = false
    String shellcheckBinary = resolveShellcheckBinary()

    private static String resolveShellcheckBinary() {
        def path = System.getenv('SHELLCHECK_PATH')
        if (path != null) return path
        try {
            def proc = ["which", "shellcheck"].execute()
            proc.waitFor()
            if (proc.exitValue() == 0) return proc.text.trim()
        } catch (ignored) {}
        return "shellcheck"
    }

    def "works on Gradle #gradleVersion"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        when:
        def arguments = ["shellcheck", "--stacktrace"]
        if (GradleVersion.version(gradleVersion) < GradleVersion.version("9.0")) {
            def javaHome = findCompatibleJavaHome()
            if (javaHome != null) {
                arguments += ["-Dorg.gradle.java.home=${javaHome}".toString()]
            }
        }

        def gradleRunner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(testProjectDir.root)
                .withGradleVersion(gradleVersion)

        then:
        gradleRunner.build()

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
