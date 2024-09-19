package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.TaskOutcome

abstract class BaseShellcheckPluginFuncTest extends BaseInfraTest {

    def "fail the build when some scripts in the folder have violations"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
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

    def "fail the build when some scripts given with sourceFiles contain violations"() {
        given:
        buildFile << """
shellcheck {
    sourceFiles = fileTree("${resources.absolutePath}/with_violations") {
        include("script_with_violations_wrong_extension.txt")
    }
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        when:
        def result = runner().buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 1")
        result.getOutput().contains("Shellcheck violations by severity: 2")

        def report = new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").text
        assert report.contains("script_with_violations_wrong_extension.txt");
    }

    def "pass the build when no script in the folder has violations"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        expect:
        runner().build()
    }

    def "sources and sourceFiles can be used together"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    sourceFiles = fileTree("${resources.absolutePath}/with_violations") {
        include("script_with_violations_wrong_extension.txt")
    }
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""
        when:
        def result = runner().buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 9")
        result.getOutput().contains("Shellcheck violations by severity: 3")

        def report = new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").text
        ["script_with_violations.bash", "script_with_violations.bash_login", "script_with_violations.bash_logout",
         "script_with_violations.bash_profile", "script_with_violations.bashrc", "script_with_violations.ksh",
         "script_with_violations.sh", "script_with_violations_2.sh", "script_with_violations_wrong_extension.txt"].each {
            assert report.contains(it)
        }
    }

    def "pass the build when no script given with sourceFiles has violations"() {
        given:
        buildFile << """
shellcheck {
    sourceFiles = fileTree("${resources.absolutePath}/without_violations") {
        exclude("**/*.txt")
    }
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""
        expect:
        runner().build()
    }


    def "ensure cacheability"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations", "${resources.absolutePath}/another_without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
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

        and:
        runnerWithBuildCache().build().task(":shellcheck").outcome == TaskOutcome.FROM_CACHE

        when:
        new File("${resources.absolutePath}/another_without_violations/another_script_without_violations.sh") << """#!/usr/bin/env bash

ls /etc
"""

        then:
        runnerWithBuildCache().build().task(":shellcheck").outcome == TaskOutcome.SUCCESS
    }

    def "pass the build when some scripts in the folder have violations and ignoreFailures is passed"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    isIgnoreFailures = true
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        expect:
        runner().build()
    }

    def "not fail when no source dirs are specified"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/no_shell_scripts")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        expect:
        runner().build()
    }

    def "not fail when no sourceFiles are specified"() {
        given:
        buildFile << """
shellcheck {
    sourceFiles = fileTree("${resources.absolutePath}/no_shell_scripts") {
        exclude("**/*")
    }
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        expect:
        runner().build()
    }

    def "pass the build when valid additional arguments are provided"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
    additionalArguments = "-a -x"
}
"""
        expect:
        runner().build()
    }

    def "fail the build when invalid additional arguments are provided"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
    additionalArguments = "--bad-arg"
}
"""
        expect:
        runner().buildAndFail()
    }

    def "only generate html reports"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}

tasks.withType<com.felipefzdz.gradle.shellcheck.Shellcheck>().configureEach {
    reports {
        xml.required = false
        txt.required = false
        html.required = true
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
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        when:
        runner().buildAndFail()

        then:
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.html").exists()
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.xml").exists()
        new File(testProjectDir.root, "build/reports/shellcheck/shellcheck.txt").exists()
    }

    def "filtrates by severity"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    severity = "error"
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        when:
        def result = runner().buildAndFail()

        then:
        result.getOutput().contains("Shellcheck files with violations: 1")
        result.getOutput().contains("Shellcheck violations by severity: 1")
    }
}
