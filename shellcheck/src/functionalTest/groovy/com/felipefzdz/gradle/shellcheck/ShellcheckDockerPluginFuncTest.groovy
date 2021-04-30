package com.felipefzdz.gradle.shellcheck

import org.gradle.testkit.runner.GradleRunner

class ShellcheckDockerPluginFuncTest extends BaseShellcheckPluginFuncTest {
    boolean useDocker = true
    String shellcheckBinary = "shellcheck"

    def "provide proper error message when failing for reasons unrelated to shellcheck itself"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    shellcheckVersion = "vvvvv0.7.1"
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary" 
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
    sources = files("${resources.absolutePath}/with_violations")
    shellcheckVersion = "v0.7.0"
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary" 
}
"""

        when:
        def result = runnerWithDebugLogging().buildAndFail()

        then:
        result.output.contains("shellcheck-alpine:v0.7.0")

    }

    def "shellcheck task can be loaded from the configuration cache"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations", "${resources.absolutePath}/another_without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary" 
}
"""

        when:
        runnerWithConfigurationCache().build()

        and:
        def result = runnerWithConfigurationCache().build()

        then:
        result.output.contains('Reusing configuration cache.')
    }

    private GradleRunner runnerWithConfigurationCache() {
        runner(false, false, true)
    }
}