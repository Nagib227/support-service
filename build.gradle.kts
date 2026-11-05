import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "ru.ithub"
version = "0.1.0"
description = "Сервис обработки заявок технической поддержки"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" &&
            (requested.name.startsWith("kotlin-stdlib") || requested.name == "kotlin-reflect")
        ) {
            useVersion("2.2.21")
            because("Версия стандартной библиотеки должна совпадать с компилятором Kotlin")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("file.encoding", "UTF-8")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("support-service.jar")
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.25.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        target("src/**/*.kt")
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
