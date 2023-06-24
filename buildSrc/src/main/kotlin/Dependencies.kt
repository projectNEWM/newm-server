object Dependencies {

    object VersionsPlugin {
        const val VERSION = "0.45.0"
        const val ID = "com.github.ben-manes.versions"
    }

    object ShadowPlugin {
        const val VERSION = "7.1.2"
        const val ID = "com.github.johnrengelman.shadow"
    }

    object KtlintPlugin {
        const val VERSION = "11.3.1"
        const val ID = "org.jlleitschuh.gradle.ktlint"
    }

    object KotlinPlugin {
        const val VERSION = "1.8.21"
        const val JVM_ID = "jvm"
        const val SERIALIZATION_ID = "plugin.serialization"
    }

    object ProtobufPlugin {
        const val VERSION = "0.8.18"
        const val ID = "com.google.protobuf"
    }

    object PublishPlugin {
        const val SIGNING_ID = "signing"
        const val MAVEN_PUBLISH_ID = "maven-publish"

        const val MAVEN_REPO_AUTH_VERSION = "3.0.4"
        const val MAVEN_REPO_AUTH_ID = "org.hibernate.build.maven-repo-auth"
    }

    object Kotlin {
        private const val VERSION = KotlinPlugin.VERSION

        const val STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$VERSION"
        const val REFLECTION = "org.jetbrains.kotlin:kotlin-reflect:$VERSION"
    }

    object KotlinXSerialization {
        private const val VERSION = "1.5.0"

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

    object KtLint {
        const val VERSION = "0.48.2"
    }

    object Ktor {
        private const val VERSION = "2.3.0"

        const val SERVER_CORE = "io.ktor:ktor-server-core:$VERSION"
        const val SERVER_CIO = "io.ktor:ktor-server-cio:$VERSION"
        const val SERVER_CONTENT_NEGOTIATION = "io.ktor:ktor-server-content-negotiation:$VERSION"
        const val SERVER_HOST_COMMON = "io.ktor:ktor-server-host-common:$VERSION"
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
        private const val VERSION = "1.4.7"

        const val CLASSIC = "ch.qos.logback:logback-classic:$VERSION"
    }

    object Koin {
        private const val VERSION = "3.4.0"
        private const val KTOR_VERSION = "3.4.0"
        private const val TEST_JUNIT_VERSION = "3.4.0"

        const val KTOR = "io.insert-koin:koin-ktor:$KTOR_VERSION"
        const val TEST = "io.insert-koin:koin-test:$VERSION"
        const val JUNIT = "io.insert-koin:koin-test-junit5:$TEST_JUNIT_VERSION"
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
        private const val VERSION = "42.6.0"

        const val ALL = "org.postgresql:postgresql:$VERSION"
    }

    object KtorFlyway {
        private const val VERSION = "2.0.0"

        const val ALL = "io.newm:ktor-flyway-feature:$VERSION"
    }

    object FlywayDB {
        private const val VERSION = "9.17.0"

        const val ALL = "org.flywaydb:flyway-core:$VERSION"
    }

    object Caffeine {
        private const val VERSION = "3.1.6"

        const val ALL = "com.github.ben-manes.caffeine:caffeine:$VERSION"
    }

    // https://github.com/patrickfav/bcrypt
    object JBCrypt {
        private const val VERSION = "0.10.2"

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
        private const val VERSION = "6.0.3"

        const val CORE = "org.springframework.security:spring-security-core:$VERSION"
    }

    // https://github.com/cloudinary/cloudinary_java
    object Cloudinary {
        private const val VERSION = "1.33.0"

        const val ALL = "com.cloudinary:cloudinary-http44:$VERSION"
    }

    object Aws {
        private const val VERSION = "1.12.457"
        private const val VERSION2 = "2.20.84"
        private const val JAXB_VERSION = "2.3.1"

        const val BOM = "com.amazonaws:aws-java-sdk-bom:$VERSION"
        const val S3 = "com.amazonaws:aws-java-sdk-s3"
        const val SQS = "com.amazonaws:aws-java-sdk-sqs"
        const val KMS = "com.amazonaws:aws-java-sdk-kms"
        const val SECRETS_MANAGER = "com.amazonaws:aws-java-sdk-secretsmanager"
        const val JAXB = "javax.xml.bind:jaxb-api:$JAXB_VERSION"

        const val BOM2 = "software.amazon.awssdk:bom:$VERSION2"
        const val CLOUDFRONT = "software.amazon.awssdk:cloudfront"
    }

    object Arweave {
        private const val ARWEAVE4S_VERSION = "0.21.0"
        private const val SCALA_JAVA8_COMPAT_VERSION = "1.0.2"

        const val ARWEAVE4S = "co.upvest:arweave4s-core_2.12:$ARWEAVE4S_VERSION"
        const val SCALA_JAVA8_COMPAT = "org.scala-lang.modules:scala-java8-compat_2.12:$SCALA_JAVA8_COMPAT_VERSION"
    }

    object JUnit {
        private const val VERSION = "5.9.3"

        const val JUPITER = "org.junit.jupiter:junit-jupiter:$VERSION"
    }

    object Cbor {
        private const val VERSION = "0.01.04-NEWM"

        const val CBOR = "io.newm:com.google.iot.cbor:$VERSION"
    }

    object Mockk {
        private const val VERSION = "1.13.5"

        const val MOCKK = "io.mockk:mockk:$VERSION"
    }

    object GoogleTruth {
        private const val VERSION = "1.1.3"

        const val TRUTH = "com.google.truth:truth:$VERSION"
    }

    object Sentry {
        private const val VERSION = "6.18.0"

        const val CORE = "io.sentry:sentry:$VERSION"
        const val LOGBACK = "io.sentry:sentry-logback:$VERSION"
    }

    object Newm {
        private const val VERSION = "1.0.3"

        const val KOGMIOS = "io.newm:kogmios:$VERSION"
    }

    object Grpc {
        private const val VERSION = "1.54.1"

        const val STUB = "io.grpc:grpc-stub:$VERSION"
        const val API = "io.grpc:grpc-api:$VERSION"
        const val GRPC = "io.grpc:protoc-gen-grpc-java:$VERSION"
        const val PROTOBUF = "io.grpc:grpc-protobuf:$VERSION"
        const val NETTY = "io.grpc:grpc-netty:$VERSION"
    }

    object GrpcKotlin {
        private const val VERSION = "1.3.0"

        const val STUB = "io.grpc:grpc-kotlin-stub:$VERSION"
        const val GRPCKT = "io.grpc:protoc-gen-grpc-kotlin:$VERSION:jdk8@jar"
    }

    object Protobuf {
        private const val VERSION = "3.22.3"

        const val JAVA_UTIL = "com.google.protobuf:protobuf-java-util:$VERSION"
        const val KOTLIN = "com.google.protobuf:protobuf-kotlin:$VERSION"
        const val PROTOC = "com.google.protobuf:protoc:$VERSION"
    }

    object Quartz {
        private const val VERSION = "2.3.2"

        const val ALL = "org.quartz-scheduler:quartz:$VERSION"
    }

    object SSLKickstart {
        private const val VERSION = "7.4.11"

        const val PEM = "io.github.hakky54:sslcontext-kickstart-for-pem:$VERSION"
        const val NETTY = "io.github.hakky54:sslcontext-kickstart-for-netty:$VERSION"
    }

    object TestContainers {
        private const val VERSION = "1.18.0"

        const val CORE = "org.testcontainers:testcontainers:$VERSION"
        const val JUINT = "org.testcontainers:junit-jupiter:$VERSION"
        const val POSTGRESQL = "org.testcontainers:postgresql:$VERSION"
    }

    object Typesafe {
        private const val VERSION = "1.4.2"
        const val CONFIG = "com.typesafe:config:$VERSION"
    }
}
