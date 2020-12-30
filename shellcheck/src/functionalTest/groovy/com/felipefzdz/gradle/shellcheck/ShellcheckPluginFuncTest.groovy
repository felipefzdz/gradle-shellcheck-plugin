package com.felipefzdz.gradle.shellcheck

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ShellcheckPluginFuncTest extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File resources
    File localBuildCacheDirectory

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
        ["docker", "image", "rm", "koalaman/shellcheck-alpine:v0.7.1"].execute().waitForProcessOutput()
    }

    def "fail the build when some scripts in the folder have violations"() {
        given:
        buildFile <<"""
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
}
"""

        when:
        def result = runner().buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 8")
        result.getOutput().contains("Shellcheck violations by severity: 3")

        def report = new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").text
        ["script_with_violations.bash", "script_with_violations.bash_login", "script_with_violations.bash_logout",
         "script_with_violations.bash_profile", "script_with_violations.bashrc", "script_with_violations.ksh",
         "script_with_violations.sh", "script_with_violations_2.sh"].each {
            assert report.contains(it)
        }
        !report.contains("script_with_violations_wrong_extension.txt")
    }

    def "pass the build when no script in the folder has violations"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/without_violations")
}
"""

        expect:
        runner().build()
    }

    def "ensure cacheability"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/without_violations")
}
"""

        expect:
        runnerWithBuildCache().build().task(":shellcheck").outcome == TaskOutcome.SUCCESS

        and:
        runnerWithBuildCache().build().task(":shellcheck").outcome == TaskOutcome.FROM_CACHE

        when:
        new File("${resources.absolutePath}/without_violations/script_without_violations.sh") << "ls /"

        then:
        runnerWithBuildCache().build().task(":shellcheck").outcome == TaskOutcome.SUCCESS
    }

    def "pass the build when some scripts in the folder have violations and ignoreFailures is passed"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
    isIgnoreFailures = true
}
"""

        expect:
        runner().build()
    }

    def "not fail when no files are specified"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/no_shell_scripts")
}
"""

        expect:
        runner().build()
    }

    def "only generate html reports"() {

        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
}

tasks.withType<com.felipefzdz.gradle.shellcheck.Shellcheck>().configureEach {
    reports {
        xml.isEnabled = false
        txt.isEnabled = false
        html.isEnabled = true
    }
}
"""
        when:
        runner().buildAndFail()

        then:
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").exists()
        !new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.xml").exists()
        !new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.txt").exists()
    }

    def "generate html, xml and txt reports by default"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
}
"""

        when:
        runner().buildAndFail()

        then:
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").exists()
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.xml").exists()
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.txt").exists()
    }

    def "provide proper error message when failing for reasons unrelated to shellcheck itself"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
    shellcheckVersion = "vvvvv0.7.1"
}
"""

        when:
        def result = runner().buildAndFail()

        then:
        result.output.contains("Unable to find image")
    }

    def "run with configured shellcheck version"() {
        given:
        buildFile << """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
    shellcheckVersion = "v0.7.0"
}
"""

        when:
        def result = runnerWithDebugLogging().buildAndFail()

        then:
        result.output.contains("shellcheck-alpine:v0.7.0")
    }

    private GradleRunner runnerWithDebugLogging() {
        runner(true)
    }

    private GradleRunner runnerWithBuildCache() {
        runner(false, true)
    }

    def "filtrates by severity"() {
        given:
        buildFile <<  """
shellcheck {
    source = file("${resources.absolutePath}/with_violations")
    severity = "error"
}
"""

        when:
        def result = runner().buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 1")
        result.getOutput().contains("Shellcheck violations by severity: 1")
    }


    private GradleRunner runner(boolean withDebugLogging = false, boolean withBuildCache = false) {
        def arguments = ["shellcheck", "--stacktrace"]
        if (withDebugLogging) {
            arguments << "--debug"
        }
        if (withBuildCache) {
            arguments.add(0, "clean")
            arguments << "--build-cache"
        }
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(arguments)
            .withProjectDir(testProjectDir.root)
            .withDebug(true)
    }
}
