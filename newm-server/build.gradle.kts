import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java.sourceCompatibility = JavaVersion.VERSION_16
java.targetCompatibility = JavaVersion.VERSION_16

object Versions {
    const val commonsLogging = "1.2"
    const val coroutines = "1.6.0"
    const val googleTruth = "1.1.3"
    const val junit = "5.8.2"
    const val kotlin = "1.6.10"
    const val kotlinxIo = "0.1.16"
    const val kotlinxSerialization = "1.3.2"
    const val ktor = "2.0.0-beta-1"
    const val logback = "1.2.10"
    const val mockk = "1.12.2"
}

application {
    mainClass.set("io.projectnewm.ApplicationKt")
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("io.ktor:ktor-server-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-server-core:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization:${Versions.ktor}")
    implementation("io.ktor:ktor-server-sessions:${Versions.ktor}")
//    implementation("io.ktor:ktor-auth:${Versions.ktor}")
//    implementation("io.ktor:ktor-auth-jwt:${Versions.ktor}")
//    implementation("io.ktor:ktor-locations:${Versions.ktor}")
//    implementation("io.ktor:ktor-html-builder:${Versions.ktor}")
//    implementation("io.ktor:ktor-client-cio-jvm:${Versions.ktor}")
//    implementation("io.ktor:ktor-client-serialization:${Versions.ktor}")
//    implementation("io.ktor:ktor-client-logging:${Versions.ktor}")
//    implementation("io.ktor:ktor-client-okhttp:${Versions.ktor}")

    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
//    implementation("com.google.iot.cbor:cbor:${Versions.cbor}")
//    implementation("commons-codec:commons-codec:${Versions.commonsCodec}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Versions.kotlinxIo}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}")

//    implementation("org.postgresql:postgresql:${Versions.postgresql}")
//    implementation("org.jetbrains.exposed:exposed-core:${Versions.exposed}")
//    implementation("org.jetbrains.exposed:exposed-jdbc:${Versions.exposed}")
//    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
//    implementation("com.viartemev:ktor-flyway-feature:${Versions.ktorFlyway}")
//    implementation("org.flywaydb:flyway-core:${Versions.flyway}")

//    implementation("org.springframework.security:spring-security-core:${Versions.springSecurity}") {
//        exclude(group = "org.springframework")
//    }
//    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncycastle}")
//    implementation("com.muquit.libsodiumjna:libsodium-jna:1.1.0-IOG-SNAPSHOT")

    implementation("commons-logging:commons-logging:${Versions.commonsLogging}")
//    implementation("com.github.ben-manes.caffeine:caffeine:${Versions.caffeine}")
//    implementation("org.mvel:mvel2:${Versions.mvel}")


    testImplementation("io.ktor:ktor-server-tests:${Versions.ktor}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("com.google.truth:truth:${Versions.googleTruth}")
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junit}")
//    testImplementation("com.bloxbean.cardano:cardano-client-lib:${Versions.cardanoClientLib}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
}

tasks {
    shadowJar {
        // defaults to project.name
        //archiveBaseName.set("${project.name}-fat")

        // defaults to all, so removing this overrides the normal, non-fat jar
        archiveClassifier.set("")
    }
}
