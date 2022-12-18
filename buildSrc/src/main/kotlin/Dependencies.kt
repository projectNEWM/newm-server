object Dependencies {

    object VersionsPlugin {
        const val VERSION = "0.41.0"
        const val ID = "com.github.ben-manes.versions"
    }

    object ShadowPlugin {
        const val VERSION = "7.1.2"
        const val ID = "com.github.johnrengelman.shadow"
    }

    object KlintPlugin {
        const val VERSION = "10.3.0"
        const val ID = "org.jlleitschuh.gradle.ktlint"
    }

    object KotlinPlugin {
        const val VERSION = "1.7.22"
        const val JVM_ID = "jvm"
        const val SERIALIZATION_ID = "plugin.serialization"
    }

    object Kotlin {
        private const val VERSION = KotlinPlugin.VERSION

        const val STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$VERSION"
        const val REFLECTION = "org.jetbrains.kotlin:kotlin-reflect:$VERSION"
    }

    object KotlinXSerialization {
        private const val VERSION = "1.4.1"

        const val JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:$VERSION"
    }

    object KotlinXDateTime {
        private const val VERSION = "0.4.0"

        const val DATETIME = "org.jetbrains.kotlinx:kotlinx-datetime:$VERSION"
    }

    object Coroutines {
        private const val VERSION = "1.6.4"

        const val CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$VERSION"
        const val JDK8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$VERSION"
        const val TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$VERSION"
    }

    object Ktor {
        private const val VERSION = "2.2.1"

        const val SERVER_CORE = "io.ktor:ktor-server-core:$VERSION"
        const val SERVER_CIO = "io.ktor:ktor-server-cio:$VERSION"
        const val SERVER_CONTENT_NEGOTIATION = "io.ktor:ktor-server-content-negotiation:$VERSION"
        const val SERVER_LOCATIONS = "io.ktor:ktor-server-locations:$VERSION"
        const val SERVER_CALL_LOGGING = "io.ktor:ktor-server-call-logging:$VERSION"
        const val SERVER_STATUS_PAGES = "io.ktor:ktor-server-status-pages:$VERSION"
        const val SERVER_CORS = "io.ktor:ktor-server-cors:$VERSION"
        const val SERVER_AUTH = "io.ktor:ktor-server-auth:$VERSION"
        const val SERVER_AUTH_JWT = "io.ktor:ktor-server-auth-jwt:$VERSION"
        const val SERVER_HTML_BUILDER = "io.ktor:ktor-server-html-builder:$VERSION"
        const val CLIENT_CORE = "io.ktor:ktor-client-core:$VERSION"
        const val CLIENT_CIO = "io.ktor:ktor-client-cio:$VERSION"
        const val CLIENT_CONTENT_NEGOTIATION = "io.ktor:ktor-client-content-negotiation:$VERSION"
        const val CLIENT_SERIALIZATION = "io.ktor:ktor-client-serialization:$VERSION"
        const val SERIALIZATION = "io.ktor:ktor-serialization:$VERSION"
        const val SERIALIZATION_JSON = "io.ktor:ktor-serialization-kotlinx-json:$VERSION"
        const val SERVER_TESTS = "io.ktor:ktor-server-tests:$VERSION"
    }

    object LogBack {
        private const val VERSION = "1.4.5"

        const val CLASSIC = "ch.qos.logback:logback-classic:$VERSION"
    }

    object Koin {
        private const val VERSION = "3.2.2"

        const val KTOR = "io.insert-koin:koin-ktor:$VERSION"
        const val TEST = "io.insert-koin:koin-test:$VERSION"
        const val JUNIT = "io.insert-koin:koin-test-junit5:$VERSION"
    }

    object Exposed {
        private const val VERSION = "0.41.1"

        const val CORE = "org.jetbrains.exposed:exposed-core:$VERSION"
        const val DAO = "org.jetbrains.exposed:exposed-dao:$VERSION"
        const val JDBC = "org.jetbrains.exposed:exposed-jdbc:$VERSION"
        const val TIME = "org.jetbrains.exposed:exposed-java-time:$VERSION"
    }

    object HikariCP {
        private const val VERSION = "5.0.1"

        const val ALL = "com.zaxxer:HikariCP:$VERSION"
    }

    object PostgreSQL {
        private const val VERSION = "42.5.1"

        const val ALL = "org.postgresql:postgresql:$VERSION"
    }

    object KtorFlyway {
        private const val VERSION = "2.0.0"

        const val ALL = "io.newm:ktor-flyway-feature:$VERSION"
    }

    object FlywayDB {
        private const val VERSION = "9.10.2"

        const val ALL = "org.flywaydb:flyway-core:$VERSION"
    }

    object Caffeine {
        private const val VERSION = "3.1.2"

        const val ALL = "com.github.ben-manes.caffeine:caffeine:$VERSION"
    }

    // https://github.com/patrickfav/bcrypt
    object JBCrypt {
        private const val VERSION = "0.9.0"

        const val ALL = "at.favre.lib:bcrypt:$VERSION"
    }

    // https://commons.apache.org/proper/commons-email/
    object ApacheCommonsEmail {
        private const val VERSION = "1.5"

        const val ALL = "org.apache.commons:commons-email:$VERSION"
    }

    object ApacheCommonsCodec {
        private const val VERSION = "1.15"

        const val ALL = "commons-codec:commons-codec:$VERSION"
    }

    object BouncyCastle {
        private const val VERSION = "1.70"

        const val BCPROV = "org.bouncycastle:bcprov-jdk15on:$VERSION"
    }

    object SpringSecurity {
        private const val VERSION = "6.0.0"

        const val CORE = "org.springframework.security:spring-security-core:$VERSION"
    }

    // https://github.com/cloudinary/cloudinary_java
    object Cloudinary {
        private const val VERSION = "1.33.0"

        const val ALL = "com.cloudinary:cloudinary-http44:$VERSION"
    }

    object Aws {
        private const val VERSION = "1.12.376"

        const val BOM = "com.amazonaws:aws-java-sdk-bom:$VERSION"
        const val S3 = "com.amazonaws:aws-java-sdk-s3"
        const val SQS = "com.amazonaws:aws-java-sdk-sqs"
    }

    object JUnit {
        private const val VERSION = "5.9.1"

        const val JUPITER = "org.junit.jupiter:junit-jupiter:$VERSION"
    }

    object LibSodiumJNA {
        private const val VERSION = "1.1.0-NEWM"

        const val SODIUM = "io.newm:com.muquit.libsodiumjna.libsodium-jna:$VERSION"
    }

    object Cbor {
        private const val VERSION = "0.01.04-NEWM"

        const val CBOR = "io.newm:com.google.iot.cbor:$VERSION"
    }

    object Mockk {
        private const val VERSION = "1.13.3"

        const val MOCKK = "io.mockk:mockk:$VERSION"
    }

    object GoogleTruth {
        private const val VERSION = "1.1.3"

        const val TRUTH = "com.google.truth:truth:$VERSION"
    }

    object Sentry {
        private const val VERSION = "6.11.0"

        const val CORE = "io.sentry:sentry:$VERSION"
        const val LOGBACK = "io.sentry:sentry-logback:$VERSION"
    }

    object Newm {
        private const val VERSION = "1.0.0"

        const val KOGMIOS = "io.newm:kogmios:$VERSION"
    }

    object TestContainers {
        private const val VERSION = "1.17.6"

        const val CORE = "org.testcontainers:testcontainers:$VERSION"
        const val JUINT = "org.testcontainers:junit-jupiter:$VERSION"
        const val POSTGRESQL = "org.testcontainers:postgresql:$VERSION"
    }
}
