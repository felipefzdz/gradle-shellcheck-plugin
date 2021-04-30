package com.felipefzdz.gradle.shellcheck

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class BaseInfraTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File resources
    File localBuildCacheDirectory

    abstract boolean getUseDocker()

    abstract String getShellcheckBinary()

    def setup() {
        localBuildCacheDirectory = testProjectDir.newFolder('local-cache')
        testProjectDir.newFile('settings.gradle') << """
        buildCache {
            local {
                directory '${localBuildCacheDirectory.toURI()}'
            }
        }
    """
        buildFile = testProjectDir.newFile('build.gradle.kts')
        buildFile << """
plugins {
    id("base")
    id("com.felipefzdz.gradle.shellcheck")
}
        """
        FileUtils.copyDirectory(new File("build/resources"), testProjectDir.root)
        resources = new File(testProjectDir.root, "functionalTest")
    }

    def setupSpec() {
        if (useDocker) {
            ["docker", "image", "rm", "koalaman/shellcheck-alpine:v0.7.1"].execute().waitForProcessOutput()
        }
    }

    protected GradleRunner runnerWithDebugLogging() {
        runner(true)
    }

    protected GradleRunner runnerWithBuildCache() {
        runner(false, true)
    }

    protected GradleRunner runner(boolean withDebugLogging = false, boolean withBuildCache = false, boolean withConfigurationCache = false) {
        def arguments = ["shellcheck", "--stacktrace"]
        if (withDebugLogging) {
            arguments << "--debug"
        }
        if (withBuildCache) {
            arguments.add(0, "clean")
            arguments << "--build-cache"
        }
        if (withConfigurationCache) {
            arguments << "--configuration-cache"
        }
        def runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(testProjectDir.root)
        // https://github.com/gradle/gradle/issues/14125
        if (!withConfigurationCache) {
            runner.withDebug(true)
        }
        runner
    }
}
