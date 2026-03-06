plugins {
    id("com.gradle.develocity") version "4.3.2"
}

rootProject.name = "gradle-shellcheck-plugin"
include("shellcheck")

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}
