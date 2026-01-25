import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.ShadowPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

ktlint {
    version.set(Dependencies.KtLint.VERSION)
}

application {
    mainClass.set("io.newm.server.ApplicationKt")
}

val integTest by sourceSets.creating
configurations[integTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(project(":newm-shared"))
    implementation(project(":newm-chain-grpc"))
    implementation(project(":newm-chain-util"))
    implementation(project(":newm-tx-builder"))
    implementation(project(":ardrive-turbo-kotlin"))
    implementation(Dependencies.Newm.KOGMIOS)

    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Grpc.NETTY)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK9)

    implementation(Dependencies.KotlinXSerialization.JSON)

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_CIO)
    implementation(Dependencies.Ktor.SERVER_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.SERVER_CALL_LOGGING)
    implementation(Dependencies.Ktor.SERVER_AUTH)
    implementation(Dependencies.Ktor.SERVER_AUTH_JWT)
    implementation(Dependencies.Ktor.SERVER_HTML_BUILDER)
    implementation(Dependencies.Ktor.SERVER_COMPRESSION)
    implementation(Dependencies.Ktor.SERVER_COMPRESSION_ZSTD)
    implementation(Dependencies.Ktor.CLIENT_CORE)
    implementation(Dependencies.Ktor.CLIENT_CIO)
    implementation(Dependencies.Ktor.CLIENT_OKHTTP)
    implementation(Dependencies.Ktor.CLIENT_AUTH)
    implementation(Dependencies.Ktor.CLIENT_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.CLIENT_SERIALIZATION)
    implementation(Dependencies.Ktor.CLIENT_LOGGING)
    implementation(Dependencies.Ktor.SERIALIZATION)
    implementation(Dependencies.Ktor.SERIALIZATION_JSON)
    implementation(Dependencies.Ktor.SERVER_STATUS_PAGES)
    implementation(Dependencies.Ktor.SERVER_CORS)
    implementation(Dependencies.Ktor.SERVER_FORWARDED_HEADER)
    implementation(Dependencies.Ktor.SERVER_SWAGGER)
    implementation(Dependencies.Swagger.SWAGGER_CODEGEN_GENERATORS)

    implementation(Dependencies.Koin.KTOR)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Exposed.CORE)
    implementation(Dependencies.Exposed.DAO)
    implementation(Dependencies.Exposed.JDBC)
    implementation(Dependencies.Exposed.TIME)
    implementation(Dependencies.Zensum.HEALTH_CHECK)

    implementation(Dependencies.HikariCP.ALL)
    implementation(Dependencies.PostgreSQL.ALL)
    implementation(Dependencies.KtorFlyway.ALL)
    implementation(Dependencies.FlywayDB.CORE)
    implementation(Dependencies.FlywayDB.POSTGRES)
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
    implementation(Dependencies.Aws.CLOUDFRONT)
    implementation(Dependencies.Aws.EC2)
    implementation(Dependencies.Aws.IMDS)

    implementation(Dependencies.ApacheTika.CORE)
    implementation(Dependencies.JAudioTagger.ALL)
    implementation(Dependencies.JSoup.ALL)
    implementation(Dependencies.QRCodeKotlin.ALL)
    implementation(Dependencies.ApacheCurators.RECEIPES)
    implementation(Dependencies.ApacheCommonsNet.ALL)
    implementation(Dependencies.KotlinLogging.ALL)

    testImplementation(Dependencies.Ktor.CLIENT_LOGGING)
    testImplementation(platform(Dependencies.JUnit.BOM))
    testImplementation(Dependencies.JUnit.JUPITER_API)
    testImplementation(Dependencies.JUnit.JUPITER_PARAMS)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_ENGINE)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_PLATFORM)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Ktor.SERVER_TESTS)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Koin.JUNIT)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_ENGINE)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_PLATFORM)
    testImplementation(Dependencies.TestContainers.POSTGRESQL)

    "integTestImplementation"(project)
    "integTestImplementation"(platform(Dependencies.JUnit.BOM))
    "integTestImplementation"(Dependencies.JUnit.JUPITER_API)
    "integTestImplementation"(Dependencies.JUnit.JUPITER_PARAMS)
    "integTestRuntimeOnly"(Dependencies.JUnit.JUPITER_ENGINE)
    "integTestRuntimeOnly"(Dependencies.JUnit.JUPITER_PLATFORM)
    "integTestImplementation"(Dependencies.Typesafe.CONFIG)
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Build-Time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
            "Main-Class" to "io.newm.server.ApplicationKt"
        )
    }
}

tasks.withType<ShadowJar> {
    isZip64 = true
    // defaults to project.name
    // archiveBaseName.set("${project.name}-fat")

    // defaults to "all", so removing this overrides the normal, non-fat jar
    archiveClassifier.set("")

    // ensure gRPC stuff gets merged together first
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()

    filesNotMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    dependsOn("distZip")
    dependsOn("distTar")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs =
            listOf(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
            )
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

fun Test.configTest(instance: String) {
    useJUnitPlatform()
    // integration.time ensures that tests are run on teach invocation per https://blog.gradle.org/stop-rerunning-tests
    inputs.property("integration.time", Instant.now().toEpochMilli())
    this.testLogging {
        events =
            setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR,
            )
    }
    description = "Runs integration tests against the $instance newm instance"
    group = "verification"
    testClassesDirs = integTest.output.classesDirs
    classpath = configurations[integTest.runtimeClasspathConfigurationName] + integTest.output
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
    val propertiesMap: Map<String, *> = System.getProperties().entries.associate { it.key.toString() to it.value }
    systemProperties(propertiesMap)
    systemProperty("newm.env", instance.lowercase())
}
tasks.register<Test>("integTestGarage") {
    configTest("Garage")
}

tasks.register<Test>("integTestStudio") {
    configTest("Studio")
}
