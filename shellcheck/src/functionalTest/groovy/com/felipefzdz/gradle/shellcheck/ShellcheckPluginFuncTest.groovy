package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

class ShellcheckPluginFuncTest extends Specification {

    def "fail the build when some scripts in the folder have violations"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        def result = runner(projectDir).buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 8")
        result.getOutput().contains("Shellcheck violations by severity: 3")

        def report = new File(projectDir, "build/reports/shellcheck/shellcheck.html").text
        ["script_with_violations.bash", "script_with_violations.bash_login", "script_with_violations.bash_logout",
         "script_with_violations.bash_profile", "script_with_violations.bashrc", "script_with_violations.ksh",
         "script_with_violations.sh", "script_with_violations_2.sh"].each {
            assert report.contains(it)
        }
        !report.contains("script_with_violations_wrong_extension.txt")
    }

    def "pass the build when no script in the folder has violations"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/without_violations")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        expect:
        runner(projectDir).build()
    }

    def "pass the build when some scripts in the folder have violations and ignoreFailures is passed"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
    isIgnoreFailures = true
}
"""
        def projectDir = setupProject(shellcheckBlock)

        expect:
        runner(projectDir).build()
    }

    def "not fail when no files are specified"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/no_shell_scripts")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        expect:
        runner(projectDir).build()
    }

    def "only generate html reports"() {

        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
}

tasks.withType<com.felipefzdz.gradle.shellcheck.Shellcheck>().configureEach {
    reports {
        xml.isEnabled = false
        txt.isEnabled = false
        html.isEnabled = true
    }
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        runner(projectDir).buildAndFail()

        then:
        new File(projectDir, "build/reports/shellcheck/shellcheck.html").exists()
        !new File(projectDir, "build/reports/shellcheck/shellcheck.xml").exists()
        !new File(projectDir, "build/reports/shellcheck/shellcheck.txt").exists()
    }

    def "generate html, xml and txt reports by default"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        runner(projectDir).buildAndFail()

        then:
        new File(projectDir, "build/reports/shellcheck/shellcheck.html").exists()
        new File(projectDir, "build/reports/shellcheck/shellcheck.xml").exists()
        new File(projectDir, "build/reports/shellcheck/shellcheck.txt").exists()
    }

    def "provide proper error message when failing for reasons unrelated to shellcheck itself"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
    shellcheckVersion = "vvvvv0.7.1"
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        def result = runner(projectDir).buildAndFail()

        then:
        result.output.contains("Unable to find image")
    }

    def "run with configured shellcheck version"() {
        given:
        def shellcheckBlock = """
shellcheck {
    source = file("../../src/functionalTest/resources/with_violations")
    shellcheckVersion = "v0.7.0"
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        def result = runner(projectDir, true).buildAndFail()

        then:
        result.output.contains("shellcheck-alpine:v0.7.0")
    }


    private GradleRunner runner(File projectDir, boolean withDebugLogging = false) {
        GradleRunner runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        if (withDebugLogging) {
            runner.withArguments("shellcheck", "--stacktrace", "--debug")
        } else {
            runner.withArguments("shellcheck", "--stacktrace")
        }
        runner.withProjectDir(projectDir)
        runner.withDebug(true)
        runner
    }

    private File setupProject(String shellcheckBlock) {
        File projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        Files.createDirectories(projectDir.toPath())
        new File(projectDir, "settings.gradle.kts") << ""
        def buildFile = new File(projectDir, "build.gradle.kts")
        buildFile.text = ''
        buildFile <<
            """
plugins {
    id("com.felipefzdz.gradle.shellcheck")
}
${shellcheckBlock}
"""
        projectDir
    }
}
