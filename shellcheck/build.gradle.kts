plugins {
    `java-gradle-plugin`
    groovy
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.felipefzdz.gradle.shellcheck"
version = "1.5.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(localGroovy())
    implementation("commons-io:commons-io:2.8.0")
    testImplementation(gradleTestKit())
    testImplementation(platform("org.spockframework:spock-bom:2.0-M4-groovy-3.0"))
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.spockframework:spock-junit4")
    testImplementation("junit:junit:4.13.1")
}

gradlePlugin {
    website = "https://github.com/felipefzdz/gradle-shellcheck-plugin"
    vcsUrl = "https://github.com/felipefzdz/gradle-shellcheck-plugin.git"
    plugins {
        val shellcheck by plugins.creating {
            id = "com.felipefzdz.gradle.shellcheck"
            implementationClass = "com.felipefzdz.gradle.shellcheck.ShellcheckPlugin"
            displayName = "Shellcheck"
            description = "The Shellcheck Gradle plugin performs quality checks on your project's Shell source files using Shellcheck and generates reports from these checks."
            tags = listOf("shellcheck", "code-quality")
        }
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest") { }

configurations[functionalTestSourceSet.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
configurations[functionalTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations["runtimeOnly"])

val functionalTest = tasks.register("functionalTest", Test::class) {
    description = "Runs functional tests."
    group = "verification"

    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath

    // should find functional test output summary and use that as the timestamp comparison
    // if there should be any updates that would affect the integration test
    outputs.upToDateWhen { false }
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform()
    maxParallelForks = 4
    testLogging {
        showStandardStreams = false // true
        // events "passed", "skipped", "failed"
        showExceptions = true
        showCauses = true
        minGranularity = 2
        minGranularity = 4
        displayGranularity = 0
    }
}

tasks.named("check").configure {
    dependsOn(functionalTest)
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

tasks.named("publishPlugins").configure {
    dependsOn(functionalTest)
}
