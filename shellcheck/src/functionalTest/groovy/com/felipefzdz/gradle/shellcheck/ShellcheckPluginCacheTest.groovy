package com.felipefzdz.gradle.shellcheck

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ShellcheckPluginCacheTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder();

    File buildFile
    File resources
    File localGradleDirectory
    File localBuildCacheDirectory
    File localConfigCacheDirectory

    def setup() {
        localBuildCacheDirectory = testProjectDir.newFolder('local-cache')
        localGradleDirectory = new File(testProjectDir.root, ".gradle")
        localConfigCacheDirectory = new File(localGradleDirectory, 'configuration-cache')
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

    def clearConfigurationCache() {
        if(localConfigCacheDirectory.exists()) {
            localConfigCacheDirectory.listFiles().each { it.delete() }
            localConfigCacheDirectory.delete()
        }
    }

    def "(local binary) can clear configuration cache"() {
        given:

        when:
        clearConfigurationCache()

        then:
        //localGradleDirectory.exists()
        !localConfigCacheDirectory.exists()
    }


    def "(local binary) shellcheck task can be loaded from the configuration cache"() {
        given:
        buildFile << """
shellcheck {
    sources.set(files("${resources.absolutePath}/without_violations"))
    useDocker.set(false)
    shellcheckBinary.set("/usr/local/bin/shellcheck")
}
"""
        when:
        clearConfigurationCache()

        then:
        !localConfigCacheDirectory.exists();

        when:
        def result = runnerWithConfigurationCache().build()

        then:
        !result.output.contains('Reusing configuration cache.')
        localConfigCacheDirectory.exists();
        localConfigCacheDirectory.listFiles().size() > 0

        when:
        result = runnerWithConfigurationCache().build()

        then:
        localConfigCacheDirectory.exists();
        localConfigCacheDirectory.listFiles().size() > 0
        result.output.contains('Reusing configuration cache.')
    }

    private GradleRunner runnerWithDebugLogging() {
        runner(true)
    }

    private GradleRunner runnerWithBuildCache() {
        runner(false, true)
    }

    private GradleRunner runnerWithConfigurationCache() {
        runner(true, false, true)
    }


    private GradleRunner runner(boolean withDebugLogging = false, boolean withBuildCache = false, boolean withConfigurationCache = false) {
        def arguments = ["shellcheck", "--stacktrace"]
        if (withDebugLogging) {
            arguments << "--debug"
        }
        if (withBuildCache) {
            arguments.add(0, "clean")
            arguments << "--build-cache"
        }
        if(withConfigurationCache) {
            arguments << "--configuration-cache"
            arguments << "--configuration-cache-problems=warn"
        }
        def runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(arguments)
            .withProjectDir(testProjectDir.root)
            .withDebug(true)
        // https://github.com/gradle/gradle/issues/14125
        if (!withConfigurationCache) {
            runner.withDebug(true)
        }
        runner
    }
}
