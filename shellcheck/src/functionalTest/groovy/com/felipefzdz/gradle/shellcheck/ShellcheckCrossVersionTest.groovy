package com.felipefzdz.gradle.shellcheck

class ShellcheckCrossVersionTest extends BaseInfraTest {

    boolean useDocker = false
    String shellcheckBinary = "shellcheck"

    def "works on Gradle #gradleVersion"() {
        given:
        buildFile << """
shellcheck {
    sources = files("${resources.absolutePath}/without_violations")
    useDocker = $useDocker
    shellcheckBinary = "$shellcheckBinary"
}
"""

        expect:
        runner().withGradleVersion(gradleVersion).build()

        where:
        gradleVersion << ["7.0", "7.6.3", "8.10.1"]
    }
}
