val gradleKotlinVersion = "1.2.31"

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "kotlin-platform-jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.31")
            }
        }
    }

    repositories {
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

plugins {
    `gradle-enterprise`
}
gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}


rootProject.name = "Gradle-Sass"
