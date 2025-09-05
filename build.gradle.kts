import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    id(Dependencies.VersionsPlugin.ID) version Dependencies.VersionsPlugin.VERSION apply false
    id(Dependencies.ShadowPlugin.ID) version Dependencies.ShadowPlugin.VERSION apply false
    id(Dependencies.KtlintPlugin.ID) version Dependencies.KtlintPlugin.VERSION apply false
    id(Dependencies.ProtobufPlugin.ID) version Dependencies.ProtobufPlugin.VERSION apply false
    kotlin(Dependencies.KotlinPlugin.JVM_ID) version Dependencies.KotlinPlugin.VERSION apply false
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID) version Dependencies.KotlinPlugin.VERSION apply false
}

allprojects {
    group = "io.newm.server"
    version = "0.10.0-SNAPSHOT"
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "sonatypeReleases"
            url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
        }
    }

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }

    project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain>().configureEach {
        val service = project.extensions.getByType<JavaToolchainService>()
        val customLauncher =
            service.launcherFor {
                this.languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_21.majorVersion))
            }

        this.kotlinJavaToolchain.toolchain.use(customLauncher)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs =
                listOf(
                    "-Xjsr305=strict",
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlin.time.ExperimentalTime",
                )
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes["Main-Class"] = "io.newm.server.ApplicationKt"
        }
    }

    tasks.withType<ShadowJar> {
        isZip64 = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        maxHeapSize = "8192m"
        environment =
            mapOf(
                "AWS_ACCESS_KEY_ID" to "TEST",
                "AWS_SECRET_ACCESS_KEY" to "12345678"
            )
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
