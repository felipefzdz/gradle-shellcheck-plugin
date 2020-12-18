plugins {
    id("com.gradle.enterprise") version "3.5"
}

rootProject.name = "gradle-shellcheck-plugin"
include("plugin")

gradleEnterprise {
    buildScan {
        publishAlways()
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
