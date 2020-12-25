package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

class ShellcheckPluginFuncTest extends Specification {

    def "fail the build when some scripts in the folder have violations"() {
        given:
        def shellcheckBlock = """
shellcheck {
    shellScripts = file("../../src/functionalTest/resources/with_violations")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        def result = runner(projectDir).buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 2")
        result.getOutput().contains("Shellcheck violations by severity: 3")
        result.getOutput().contains("SC1083") // Code that will be excluded in the next tests
    }

    def "pass the build when no script in the folder has violations"() {
        given:
        def shellcheckBlock = """
shellcheck {
    shellScripts = file("../../src/functionalTest/resources/without_violations")
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
    shellScripts = file("../../src/functionalTest/resources/with_violations")
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
    shellScripts = file("../../src/functionalTest/resources/no_shell_scripts")
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
    shellScripts = file("../../src/functionalTest/resources/with_violations")
}

tasks.withType<com.felipefzdz.gradle.shellcheck.Shellcheck>().configureEach {
    reports {
        xml.isEnabled = false
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
    }

    def "generate html and xml reports by default"() {

        given:
        def shellcheckBlock = """
shellcheck {
    shellScripts = file("../../src/functionalTest/resources/with_violations")
}
"""
        def projectDir = setupProject(shellcheckBlock)

        when:
        runner(projectDir).buildAndFail()

        then:
        new File(projectDir, "build/reports/shellcheck/shellcheck.html").exists()
        new File(projectDir, "build/reports/shellcheck/shellcheck.xml").exists()
    }

    private GradleRunner runner(File projectDir) {
        GradleRunner runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("shellcheck", "--stacktrace")
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
