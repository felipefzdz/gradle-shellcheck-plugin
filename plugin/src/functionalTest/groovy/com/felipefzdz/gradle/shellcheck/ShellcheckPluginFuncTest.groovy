package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

class ShellcheckPluginFuncTest extends Specification {

    def "fail the build when the script has violations"() {
        given:
        def projectDir = setupProject('script_with_violations.sh')

        when:
        def result = runner(projectDir).buildAndFail()

        then:
        result.getOutput().contains("3 messages in total")
        result.getOutput().contains("1 messages of type 2154")
        result.getOutput().contains("2 messages of type 2086")
        result.getOutput().contains("Shellcheck violations were found.")
    }

    def "pass the build when the script has not violations"() {
        given:
        def projectDir = setupProject('script_without_violations.sh')

        when:
        def result = runner(projectDir).build()

        then:
        result.getOutput().contains("0 messages in total")
    }


    private GradleRunner runner(File projectDir) {
        GradleRunner runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("shellcheck")
        runner.withProjectDir(projectDir)
        runner.withDebug(true)
        runner
    }

    private File setupProject(String fileToCheck) {
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
    fileToCheck = '$fileToCheck'
}
"""
        projectDir
    }
}
