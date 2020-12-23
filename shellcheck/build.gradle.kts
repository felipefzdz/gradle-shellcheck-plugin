plugins {
    `java-gradle-plugin`
    `groovy`
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "com.felipefzdz.gradle.shellcheck"
version = "0.2.8"

repositories {
    jcenter()
}

dependencies {
    implementation("org.codehaus.groovy:groovy:2.5.12")
    implementation("com.github.docker-java:docker-java-core:3.2.7")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.2.7")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
}

gradlePlugin {
    val shellcheck by plugins.creating {
        id = "com.felipefzdz.gradle.shellcheck"
        implementationClass = "com.felipefzdz.gradle.shellcheck.ShellcheckPlugin"
        displayName = "Shellcheck"
        description = "The Shellcheck Gradle plugin performs quality checks on your project's Shell source files using Shellcheck and generates reports from these checks."
    }
}

pluginBundle {
    website = "https://github.com/felipefzdz/gradle-shellcheck-plugin"
    vcsUrl = "https://github.com/felipefzdz/gradle-shellcheck-plugin.git"
    tags = listOf("shellcheck", "code-quality")
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.check {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
