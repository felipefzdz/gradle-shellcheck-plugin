package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

class ShellcheckPluginFuncTest extends Specification {

    def "hello"() {
        given:
        File projectDir = new File("build/functionalTest")
        Files.createDirectories(projectDir.toPath())
        new File(projectDir, "settings.gradle") << ""
        def buildFile = new File(projectDir, "build.gradle")
        buildFile << ""
        buildFile <<
            """
plugins {
    id('com.felipefzdz.gradle.shellcheck')
}"""

        when:
        GradleRunner runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("shellcheck")
        runner.withProjectDir(projectDir)
        BuildResult result = runner.build()

        then:
        result.getOutput().contains("Hello from plugin 'com.felipefzdz.gradle.shellcheck'")
    }
}
