import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val junitJupiterVersion = "5.7.1"
val jqwikVersion = "1.5.0"

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("com.stepango.aar2jar") version "0.6"
}

group = "com.github.tarcv.testingteam"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-log4j12:1.7.30")

    implementationAar("androidx.test.uiautomator:uiautomator:2.2.0")
    testImplementationAar("androidx.test.uiautomator:uiautomator:2.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitJupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitJupiterVersion}")
    testImplementation("net.jqwik:jqwik:${jqwikVersion}")
    testImplementation(kotlin("reflect"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
tasks.forEach { task ->
    if (task is KotlinCompile) {
        task.kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = listOf("-Xinline-classes")
        }
    }
}

tasks.test {
    useJUnitPlatform {
        includeEngines.add("jqwik")
    }
    include("**/*.*")
}
