object Dependencies {

    object Kotlin {
        const val VERSION = "1.7.10"

        const val STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$VERSION"
        const val REFLECTION = "org.jetbrains.kotlin:kotlin-reflect:$VERSION"
    }

    object KotlinSerialization {
        private const val VERSION = "1.3.3"

        const val JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:$VERSION"
    }

    object Coroutines {
        private const val VERSION = "1.6.4"

        const val CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$VERSION"
        const val JDK8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$VERSION"
        const val TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$VERSION"
    }

    object Ktor {
        private const val VERSION = "2.1.0"

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
        private const val VERSION = "1.2.11"

        const val CLASSIC = "ch.qos.logback:logback-classic:$VERSION"
    }

    object Koin {
        private const val VERSION = "3.2.0"

        const val KTOR = "io.insert-koin:koin-ktor:$VERSION"
        const val TEST = "io.insert-koin:koin-test:$VERSION"
        const val JUNIT = "io.insert-koin:koin-test-junit5:$VERSION"
    }

    object Expose {
        private const val VERSION = "0.39.2"

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
        private const val VERSION = "42.4.1"

        const val ALL = "org.postgresql:postgresql:$VERSION"
    }

    object KtorFlyway {
        private const val VERSION = "2.0.0"

        const val ALL = "io.newm:ktor-flyway-feature:$VERSION"
    }

    object FlywayDB {
        private const val VERSION = "9.1.3"

        const val ALL = "org.flywaydb:flyway-core:$VERSION"
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

    // https://github.com/cloudinary/cloudinary_java
    object Cloudinary {
        private const val VERSION = "1.32.2"

        const val ALL = "com.cloudinary:cloudinary-http44:$VERSION"
    }

    object Aws {
        private const val VERSION = "1.12.280"

        const val BOM = "com.amazonaws:aws-java-sdk-bom:$VERSION"
        const val S3 = "com.amazonaws:aws-java-sdk-s3"
    }

    object JUnit {
        private const val VERSION = "5.9.0"

        const val JUPITER = "org.junit.jupiter:junit-jupiter:$VERSION"
    }

    object Mockk {
        private const val VERSION = "1.12.5"

        const val MOCKK = "io.mockk:mockk:$VERSION"
    }

    object GoogleTruth {
        private const val VERSION = "1.1.3"

        const val TRUTH = "com.google.truth:truth:$VERSION"
    }

    object Sentry {
        private const val VERSION = "6.3.1"

        const val CORE = "io.sentry:sentry:$VERSION"
        const val LOGBACK = "io.sentry:sentry-logback:$VERSION"
    }

    object TestContainers {
        private const val VERSION = "1.17.3"

        const val CORE = "org.testcontainers:testcontainers:$VERSION"
        const val JUINT = "org.testcontainers:junit-jupiter:$VERSION"
        const val POSTGRESQL = "org.testcontainers:postgresql:$VERSION"
    }
}
