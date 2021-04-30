package com.felipefzdz.gradle.shellcheck

import spock.lang.IgnoreIf

@IgnoreIf({ !os.macOs })
class ShellcheckInstallerPluginFuncTest extends BaseInfraTest {
    boolean useDocker = false
    String shellcheckBinary = "/usr/local/bin/shellcheck"

    def "install shellcheck by using the specified installer"() {
        given:
        ["brew", "uninstall", "shellcheck"].execute().waitForProcessOutput()

        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
    installer = "brew"
}
"""

        expect:
        runner().buildAndFail().getOutput().contains("Shellcheck files with violations: 8")
    }

    def "skip installing shellcheck if already present"() {
        given:
        ["brew", "uninstall", "shellcheck"].execute().waitForProcessOutput()
        ["brew", "install", "shellcheck"].execute().waitForProcessOutput()

        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
    installer = "brew"
}
"""

        when:
        def output = runnerWithDebugLogging().buildAndFail().getOutput()

        then:
        output.contains("Shellcheck files with violations: 8")
        output.contains("Shellcheck is already installed. Skipping installation.")
    }

    def "skip installing shellcheck if useDocker is true"() {
        given:
        ["brew", "uninstall", "shellcheck"].execute().waitForProcessOutput()
        ["brew", "install", "shellcheck"].execute().waitForProcessOutput()

        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/with_violations")
    useDocker = true
    shellcheckBinary = "$shellcheckBinary"
    installer = "brew"
}
"""

        when:
        def output = runnerWithDebugLogging().buildAndFail().getOutput()

        then:
        output.contains("Shellcheck files with violations: 8")
        !output.contains("brew info shellcheck returned:")
    }
}
