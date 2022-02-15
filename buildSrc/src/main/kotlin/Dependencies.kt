object Dependencies {

    object Kotlin {
        private const val VERSION = "1.6.10"

        const val STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$VERSION"
        const val REFLECTION = "org.jetbrains.kotlin:kotlin-reflect:$VERSION"
    }

    object KotlinSerialization {
        private const val VERSION = "1.3.2"

        const val JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:$VERSION"
    }

    object KotlinDateTime {
        private const val VERSION = "0.3.2"

        const val CORE = "org.jetbrains.kotlinx:kotlinx-datetime:$VERSION"
    }

    object Coroutines {
        private const val VERSION = "1.6.0"

        const val CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$VERSION"
        const val JDK8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$VERSION"
        const val TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$VERSION"
    }

    object Ktor {
        private const val VERSION = "2.0.0-beta-1"

        const val SERVER_CORE = "io.ktor:ktor-server-core:$VERSION"
        const val SERVER_CIO = "io.ktor:ktor-server-cio:$VERSION"
        const val SERVER_SESSIONS = "io.ktor:ktor-server-sessions:$VERSION"
        const val SERVER_CONTENT_NEGOTIATION = "io.ktor:ktor-server-content-negotiation:$VERSION"
        const val SERVER_CALL_LOGGING = "io.ktor:ktor-server-call-logging:$VERSION"
        const val SERVER_AUTH = "io.ktor:ktor-server-auth:$VERSION"
        const val SERVER_AUTH_JWT = "io.ktor:ktor-server-auth-jwt:$VERSION"
        const val SERVER_HTML_BUILDER = "io.ktor:ktor-server-html-builder:$VERSION"
        const val CLIENT_CIO = "io.ktor:ktor-client-cio:$VERSION"
        const val SERIALIZATION = "io.ktor:ktor-serialization:$VERSION"
        const val SERIALIZATION_JSON = "io.ktor:ktor-serialization-kotlinx-json:$VERSION"
        const val SERVER_TESTS = "io.ktor:ktor-server-tests:$VERSION"
    }

    object LogBack {
        private const val VERSION = "1.2.10"

        const val CLASSIC = "ch.qos.logback:logback-classic:$VERSION"
    }

    object CommonsLogging {
        private const val VERSION = "1.2"

        const val LOGGING = "commons-logging:commons-logging:$VERSION"
    }

    object JUnit {
        private const val VERSION = "5.8.2"

        const val JUPITER = "org.junit.jupiter:junit-jupiter:$VERSION"
    }

    object Mockk {
        private const val VERSION = "1.12.2"

        const val MOCKK = "io.mockk:mockk:$VERSION"
    }

    object GoogleTruth {
        private const val VERSION = "1.1.3"

        const val TRUTH = "com.google.truth:truth:$VERSION"
    }
}
