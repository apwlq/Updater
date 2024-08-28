plugins {
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.github.apwlq"
version = "1.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://api.simplyrin.net/maven/")
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")
    implementation("net.simplyrin.config:Config:1.4")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${rootProject.name}.jar")
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "io.github.apwlq.updater.AppKt" // Main-Class를 실제 진입점으로 설정
            )
        )
    }
}

kotlin {
    jvmToolchain(21)
}