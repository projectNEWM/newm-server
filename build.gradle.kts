import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        mavenLocal()
        maven {
            name = "jitpack.io"
            url = uri("https://jitpack.io")
        }
        mavenCentral()
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.41.0" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    kotlin("jvm") version "1.6.10" apply false
    kotlin("plugin.serialization") version "1.6.10" apply false
}

allprojects {
    group = "io.projectnewm.server"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    repositories {
        mavenLocal()
        maven {
            name = "jitpack.io"
            url = uri("https://jitpack.io")
        }
        mavenCentral()
    }

//    apply {
//        plugin("io.spring.dependency-management")
//    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<DependencyUpdatesTask> {
        // Example 1: reject all non stable versions
        rejectVersionIf {
            isNonStable(candidate.version)
        }

        // Example 2: disallow release candidates as upgradable versions from stable versions
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }

        // Example 3: using the full syntax
        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                        reject("Release candidate")
                    }
                }
            }
        }
    }

    project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain>().configureEach {
        val service = project.extensions.getByType<JavaToolchainService>()
        val customLauncher = service.launcherFor {
            this.languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.majorVersion))
        }

        this.kotlinJavaToolchain.toolchain.use(customLauncher)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
            )
            jvmTarget = "17"
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes["Main-Class"] = "io.projectnewm.server.ApplicationKt"
        }
    }

    tasks.withType<Test> {
        maxHeapSize = "8192m"
    }

}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
