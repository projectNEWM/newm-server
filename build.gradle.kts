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
    id(Dependencies.VersionsPlugin.ID) version Dependencies.VersionsPlugin.VERSION apply false
    id(Dependencies.ShadowPlugin.ID) version Dependencies.ShadowPlugin.VERSION apply false
    id(Dependencies.KtlintPlugin.ID) version Dependencies.KtlintPlugin.VERSION apply false
    id(Dependencies.ProtobufPlugin.ID) version Dependencies.ProtobufPlugin.VERSION apply false
    kotlin(Dependencies.KotlinPlugin.JVM_ID) version Dependencies.KotlinPlugin.VERSION apply false
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID) version Dependencies.KotlinPlugin.VERSION apply false
}

allprojects {
    group = "io.newm.server"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

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
                "-opt-in=kotlin.RequiresOptIn",
            )
            jvmTarget = "17"
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes["Main-Class"] = "io.newm.server.ApplicationKt"
        }
    }

    tasks.withType<Test> {
        maxHeapSize = "8192m"
        environment = mapOf(
            "CORS_HOSTS" to "",
            "NEWM_JWT_SECRET" to "",
            "DATABASE_JDBC_URL" to "",
            "DATABASE_USERNAME" to "",
            "DATABASE_PASSWORD" to "",
            "GOOGLE_CLIENT_ID" to "",
            "GOOGLE_CLIENT_SECRET" to "",
            "FACEBOOK_CLIENT_ID" to "",
            "FACEBOOK_CLIENT_SECRET" to "",
            "LINKEDIN_CLIENT_ID" to "",
            "LINKEDIN_CLIENT_SECRET" to "",
            "EMAIL_AUTH_SMTP_HOST" to "",
            "EMAIL_AUTH_SMTP_PORT" to "",
            "EMAIL_AUTH_USERNAME" to "",
            "EMAIL_AUTH_PASSWORD" to "",
            "EMAIL_AUTH_FROM" to "",
            "SENTRY_DSN" to "",
            "CLOUDINARY_URL" to "",
            "AWS_ACCESS_KEY_ID" to "",
            "AWS_SECRET_KEY" to "",
            "AWS_REGION" to "",
            "AWS_AUDIO_BUCKET" to "",
            "AWS_AUDIO_SQS_QUEUE_URL" to "",
            "AWS_AUDIO_CLOUDFRONT_HOST_URL" to "",
            "AWS_AGREEMENT_BUCKET" to "",
            "AWS_KMS_KEY_ID" to "",
            "AWS_MINTING_SQS_QUEUE_URL" to "",
            "IDENFY_API_KEY" to "",
            "IDENFY_API_SECRET" to "",
            "IDENFY_SIGNATURE_KEY" to "",
            "NEWM_CHAIN_HOST" to "",
            "NEWM_CHAIN_PORT" to "",
            "NEWM_CHAIN_JWT" to "",
        )
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// staging task for heroku
tasks.create("stage") {
    dependsOn(":newm-server:installDist")
}
