import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

plugins {
    application
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.ShadowPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

ktlint {
    version.set(Dependencies.KtLint.VERSION)
}

application {
    mainClass.set("io.newm.server.ApplicationKt")
}

val integTestImplementation by configurations.creating {
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    implementation(project(":newm-shared"))
    implementation(project(":newm-chain-grpc"))
    implementation(project(":newm-chain-util"))
    implementation(project(":newm-tx-builder"))
    implementation(Dependencies.Newm.KOGMIOS)

    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Grpc.NETTY)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK8)

    implementation(Dependencies.KotlinXSerialization.JSON)

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_CIO)
    implementation(Dependencies.Ktor.SERVER_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.SERVER_LOCATIONS)
    implementation(Dependencies.Ktor.SERVER_CALL_LOGGING)
    implementation(Dependencies.Ktor.SERVER_AUTH)
    implementation(Dependencies.Ktor.SERVER_AUTH_JWT)
    implementation(Dependencies.Ktor.SERVER_HTML_BUILDER)
    implementation(Dependencies.Ktor.CLIENT_CORE)
    implementation(Dependencies.Ktor.CLIENT_CIO)
    implementation(Dependencies.Ktor.CLIENT_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.CLIENT_SERIALIZATION)
    implementation(Dependencies.Ktor.SERIALIZATION)
    implementation(Dependencies.Ktor.SERIALIZATION_JSON)
    implementation(Dependencies.Ktor.SERVER_STATUS_PAGES)
    implementation(Dependencies.Ktor.SERVER_CORS)
    implementation(Dependencies.Ktor.SERVER_FORWARDED_HEADER)

    implementation(Dependencies.Koin.KTOR)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Exposed.CORE)
    implementation(Dependencies.Exposed.DAO)
    implementation(Dependencies.Exposed.JDBC)
    implementation(Dependencies.Exposed.TIME)

    implementation(Dependencies.HikariCP.ALL)
    implementation(Dependencies.PostgreSQL.ALL)
    implementation(Dependencies.KtorFlyway.ALL)
    implementation(Dependencies.FlywayDB.ALL)
    implementation(Dependencies.Sentry.CORE)
    implementation(Dependencies.Sentry.LOGBACK)
    implementation(Dependencies.ApacheCommonsEmail.ALL)
    implementation(Dependencies.JBCrypt.ALL)
    implementation(Dependencies.BouncyCastle.BCPROV)
    implementation(Dependencies.SpringSecurity.CORE) {
        // We don't care about other spring stuff.
        // We just like using Encryptors.stronger
        exclude(group = "org.springframework")
    }
    implementation(Dependencies.Cbor.CBOR)
    implementation(Dependencies.Caffeine.ALL)

    implementation(Dependencies.Quartz.ALL)

    implementation(Dependencies.Cloudinary.ALL)

    implementation(platform(Dependencies.Aws.BOM))
    implementation(Dependencies.Aws.S3)
    implementation(Dependencies.Aws.SQS)
    implementation(Dependencies.Aws.KMS)
    implementation(Dependencies.Aws.SECRETS_MANAGER)
    implementation(Dependencies.Aws.JAXB)

    implementation(platform(Dependencies.Aws.BOM2))
    implementation(Dependencies.Aws.CLOUDFRONT)

    implementation(Dependencies.Arweave.ARWEAVE4S)
    implementation(Dependencies.Arweave.SCALA_JAVA8_COMPAT)

    testImplementation(Dependencies.Ktor.CLIENT_LOGGING)
    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Ktor.SERVER_TESTS)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Koin.JUNIT)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
    testImplementation(Dependencies.TestContainers.POSTGRESQL)

    integTestImplementation(Dependencies.Typesafe.CONFIG)
}

tasks {
    shadowJar {
        isZip64 = true
        // defaults to project.name
        // archiveBaseName.set("${project.name}-fat")

        // defaults to all, so removing this overrides the normal, non-fat jar
        archiveClassifier.set("")

        // ensure gRPC stuff gets merged in
        mergeServiceFiles()
        dependsOn("distZip")
        dependsOn("distTar")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
        jvmTarget = "17"
    }
}

sourceSets {
    create("integTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

fun Test.configTest(instance: String) {
    description = "Runs integration tests against the $instance newm instance"
    group = "verification"
    testClassesDirs = sourceSets["integTest"].output.classesDirs
    classpath = sourceSets["integTest"].runtimeClasspath
    // Gradle makes us explicitly pass environment variables :-(
    // https://docs.gradle.org/current/userguide/common_caching_problems.html
    if ("NEWM_EMAIL" in System.getenv()) {
        environment("NEWM_EMAIL", System.getenv()["NEWM_EMAIL"] as String)
    }
    if ("NEWM_PASSWORD" in System.getenv()) {
        environment("NEWM_PASSWORD", System.getenv()["NEWM_PASSWORD"] as String)
    }
    if ("NEWM_BASEURL" in System.getenv()) {
        environment("NEWM_BASEURL", System.getenv()["NEWM_BASEURL"] as String)
    }
    systemProperty("newm.env", instance.toLowerCaseAsciiOnly())
}
task<Test>("integTestGarage") {
    configTest("Garage")
}

task<Test>("integTestStudio") {
    configTest("Studio")
}
