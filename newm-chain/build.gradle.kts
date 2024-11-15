import java.text.SimpleDateFormat
import java.util.Date

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
    mainClass.set("io.newm.chain.ApplicationKt")
}

repositories {
    maven {
        name = "sonatypeSnapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation(project(":ktor-grpc"))
    implementation(project(":newm-chain-util"))
    implementation(project(":newm-chain-db"))
    implementation(project(":newm-chain-grpc"))
    implementation(project(":newm-tx-builder"))
    implementation(project(":newm-objectpool"))
    implementation(project(":newm-shared"))
    implementation(Dependencies.Newm.KOGMIOS)
    implementation(Dependencies.Cbor.CBOR)
    implementation(Dependencies.SSLKickstart.PEM)
    implementation(Dependencies.SSLKickstart.NETTY)

    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK9)

    implementation(Dependencies.KotlinXSerialization.JSON)
    implementation(Dependencies.KotlinXDateTime.DATETIME)

    implementation(Dependencies.KotlinLogging.ALL)

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_CIO)
    implementation(Dependencies.Ktor.SERVER_AUTH_JWT)

    implementation(Dependencies.Koin.KTOR)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Caffeine.ALL)

    implementation(Dependencies.GrpcKotlin.STUB)
    implementation(Dependencies.Grpc.PROTOBUF)
    implementation(Dependencies.Grpc.NETTY)
    implementation(Dependencies.Protobuf.KOTLIN)

    implementation(Dependencies.Exposed.CORE)
    implementation(Dependencies.Exposed.DAO)
    implementation(Dependencies.Exposed.JDBC)
    implementation(Dependencies.Exposed.TIME)

    implementation(Dependencies.HikariCP.ALL)
    implementation(Dependencies.PostgreSQL.ALL)
    implementation(Dependencies.KtorFlyway.ALL)
    implementation(Dependencies.FlywayDB.CORE)
    implementation(Dependencies.FlywayDB.POSTGRES)
    implementation(Dependencies.Sentry.CORE)
    implementation(Dependencies.Sentry.LOGBACK)
    implementation(Dependencies.ApacheCommonsEmail.ALL)
    implementation(Dependencies.JBCrypt.ALL)

    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Ktor.SERVER_TESTS)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Koin.JUNIT)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
    testImplementation(Dependencies.TestContainers.POSTGRESQL)
}

tasks {
    shadowJar {
        // defaults to project.name
        // archiveBaseName.set("${project.name}-fat")

        // defaults to all, so removing this overrides the normal, non-fat jar
        archiveClassifier.set("")
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Build-Time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        )
    }
}
