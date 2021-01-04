plugins {
    id("kotlin-platform-jvm")
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "com.github.salomonbrys.gradle.sass"
version = "1.1.2.TRAVELC.56"
description = "A Gradle plugin to download & run the official dart-sass release with Gradle"

repositories {
    jcenter()
    google()
    maven(url = "https://plugins.gradle.org/m2/")
}


val kotlinVersion = "1.4.21"

dependencies {
    runtimeOnly(gradleApi())
    runtimeOnly(gradleKotlinDsl())
    implementation("de.undercouch:gradle-download-task:4.1.1")
}

pluginBundle {
    website = "https://github.com/SalomonBrys/gradle-sass"
    vcsUrl = "https://github.com/SalomonBrys/gradle-sass.git"
    tags = listOf("sass", "css", "compiler", "web")

    plugins {
        create("Gradle-Sass") {
            id = "com.github.salomonbrys.gradle.sass"
            description = "A Gradle plugin to download & run the official dart-sass release with Gradle"
            displayName = "Gradle Sass"
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            credentials {
                username  = "admin"
                password = "xxxxx"
            }
            url = uri("https://nexus.travelcdev.com/repository/maven-releases")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}