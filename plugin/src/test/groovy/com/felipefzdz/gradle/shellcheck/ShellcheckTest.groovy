package com.felipefzdz.gradle.shellcheck

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ShellcheckTest extends Specification {

    def "hello"() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        project.getPlugins().apply("com.felipefzdz.gradle.shellcheck")

        then:
        project.getTasks().findByName("shellcheck") != null
    }
}
