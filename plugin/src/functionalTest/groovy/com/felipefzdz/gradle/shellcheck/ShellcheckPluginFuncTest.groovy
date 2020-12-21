package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

class ShellcheckPluginFuncTest extends Specification {

    def "fail the build when some scripts in the folder have violations"() {
        given:
        def projectDir = setupProject('with_violations')

        when:
        def result = runner(projectDir).buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 2")
        result.getOutput().contains("Shellcheck violations by severity: 2")
    }

    def "pass the build when no script in the folder has violations"() {
        given:
        def projectDir = setupProject('without_violations')

        expect:
        runner(projectDir).build()
    }

    def "pass the build when some scripts in the folder have violations and ignoreFailures is passed"() {
        given:
        def projectDir = setupProject('with_violations', true)

        expect:
        runner(projectDir).build()
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

    private File setupProject(String folderToCheck, boolean ignoreFailures = false) {
        File projectDir = new File("build/functionalTest")
        Files.createDirectories(projectDir.toPath())
        new File(projectDir, "settings.gradle") << ""
        def buildFile = new File(projectDir, "build.gradle")
        buildFile.text = ''
        buildFile <<
            """
plugins {
    id('com.felipefzdz.gradle.shellcheck')
}
shellcheck {
    shellScripts = file("../../src/functionalTest/resources/$folderToCheck")
    ${ignoreFailures ? "ignoreFailures = true" : ""}
}
"""
        projectDir
    }
}
