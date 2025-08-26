object Dependencies {
    object VersionsPlugin {
        const val VERSION = Versions.VERSIONS_PLUGIN
        const val ID = "com.github.ben-manes.versions"
    }

    object ShadowPlugin {
        const val VERSION = Versions.SHADOW_PLUGIN
        const val ID = "com.gradleup.shadow"
    }

    object KtlintPlugin {
        const val VERSION = Versions.KTLINT_PLUGIN
        const val ID = "org.jlleitschuh.gradle.ktlint"
    }

    object KotlinPlugin {
        const val VERSION = Versions.KOTLIN_PLUGIN
        const val JVM_ID = "jvm"
        const val SERIALIZATION_ID = "plugin.serialization"
    }

    object ProtobufPlugin {
        const val VERSION = Versions.PROTOBUF_PLUGIN
        const val ID = "com.google.protobuf"
    }

    object PublishPlugin {
        const val SIGNING_ID = "signing"
        const val MAVEN_PUBLISH_ID = "com.vanniktech.maven.publish"
        const val MAVEN_PUBLISH_VERSION = Versions.MAVEN_PUBLISH
    }

    object Kotlin {
        private const val VERSION = Versions.KOTLIN_PLUGIN

        const val STDLIB_JDK8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$VERSION"
        const val REFLECTION = "org.jetbrains.kotlin:kotlin-reflect:$VERSION"
    }

    object KotlinXSerialization {
        private const val VERSION = Versions.KOTLINX_SERIALIZATION

        const val JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:$VERSION"
    }

    object Coroutines {
        private const val VERSION = Versions.COROUTINES

        const val CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$VERSION"
        const val JDK9 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:$VERSION"
        const val TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$VERSION"
    }

    object KtLint {
        const val VERSION = Versions.KTLINT
    }

    object Ktor {
        private const val VERSION = Versions.KTOR

        const val SERVER_CORE = "io.ktor:ktor-server-core:$VERSION"
        const val SERVER_CIO = "io.ktor:ktor-server-cio:$VERSION"
        const val SERVER_CONTENT_NEGOTIATION = "io.ktor:ktor-server-content-negotiation:$VERSION"
        const val SERVER_HOST_COMMON = "io.ktor:ktor-server-host-common:$VERSION"
        const val SERVER_CALL_LOGGING = "io.ktor:ktor-server-call-logging:$VERSION"
        const val SERVER_STATUS_PAGES = "io.ktor:ktor-server-status-pages:$VERSION"
        const val SERVER_CORS = "io.ktor:ktor-server-cors:$VERSION"
        const val SERVER_AUTH = "io.ktor:ktor-server-auth:$VERSION"
        const val SERVER_AUTH_JWT = "io.ktor:ktor-server-auth-jwt:$VERSION"
        const val SERVER_HTML_BUILDER = "io.ktor:ktor-server-html-builder:$VERSION"
        const val CLIENT_CORE = "io.ktor:ktor-client-core:$VERSION"
        const val CLIENT_CIO = "io.ktor:ktor-client-cio:$VERSION"
        const val CLIENT_OKHTTP = "io.ktor:ktor-client-okhttp:$VERSION"
        const val CLIENT_AUTH = "io.ktor:ktor-client-auth:$VERSION"
        const val CLIENT_CONTENT_NEGOTIATION = "io.ktor:ktor-client-content-negotiation:$VERSION"
        const val CLIENT_LOGGING = "io.ktor:ktor-client-logging:$VERSION"
        const val CLIENT_SERIALIZATION = "io.ktor:ktor-client-serialization:$VERSION"
        const val SERIALIZATION = "io.ktor:ktor-serialization:$VERSION"
        const val SERIALIZATION_JSON = "io.ktor:ktor-serialization-kotlinx-json:$VERSION"
        const val SERVER_TESTS = "io.ktor:ktor-server-test-host:$VERSION"
        const val SERVER_FORWARDED_HEADER = "io.ktor:ktor-server-forwarded-header:$VERSION"
        const val SERVER_SWAGGER = "io.ktor:ktor-server-swagger:$VERSION"
    }

    object LogBack {
        private const val VERSION = Versions.LOGBACK

        const val CLASSIC = "ch.qos.logback:logback-classic:$VERSION"
    }

    object Koin {
        private const val VERSION = Versions.KOIN
        private const val VERSION_TEST = Versions.KOIN_TEST

        const val KTOR = "io.insert-koin:koin-ktor3:$VERSION"
        const val TEST = "io.insert-koin:koin-test:$VERSION_TEST"
        const val JUNIT = "io.insert-koin:koin-test-junit5:$VERSION_TEST"
    }

    object Exposed {
        private const val VERSION = Versions.EXPOSED

        const val CORE = "org.jetbrains.exposed:exposed-core:$VERSION"
        const val DAO = "org.jetbrains.exposed:exposed-dao:$VERSION"
        const val JDBC = "org.jetbrains.exposed:exposed-jdbc:$VERSION"
        const val TIME = "org.jetbrains.exposed:exposed-java-time:$VERSION"
    }

    object HikariCP {
        private const val VERSION = Versions.HIKARICP

        const val ALL = "com.zaxxer:HikariCP:$VERSION"
    }

    object PostgreSQL {
        private const val VERSION = Versions.POSTGRESQL

        const val ALL = "org.postgresql:postgresql:$VERSION"
    }

    object KtorFlyway {
        private const val VERSION = Versions.KTOR_FLYWAY

        const val ALL = "io.newm:ktor-flyway-feature:$VERSION"
    }

    object FlywayDB {
        private const val VERSION = Versions.FLYWAYDB

        const val CORE = "org.flywaydb:flyway-core:$VERSION"
        const val POSTGRES = "org.flywaydb:flyway-database-postgresql:$VERSION"
    }

    object Caffeine {
        private const val VERSION = Versions.CAFFEINE

        const val ALL = "com.github.ben-manes.caffeine:caffeine:$VERSION"
    }

    object JBCrypt {
        private const val VERSION = Versions.JBCRYPT

        const val ALL = "at.favre.lib:bcrypt:$VERSION"
    }

    object ApacheCommonsEmail {
        private const val VERSION = Versions.APACHE_COMMONS_EMAIL

        const val ALL = "org.apache.commons:commons-email:$VERSION"
    }

    object ApacheCommonsCodec {
        private const val VERSION = Versions.APACHE_COMMONS_CODEC

        const val ALL = "commons-codec:commons-codec:$VERSION"
    }

    object ApacheCommonsNumbers {
        private const val VERSION = Versions.APACHE_COMMONS_NUMBERS

        const val FRACTION = "org.apache.commons:commons-numbers-fraction:$VERSION"
    }

    object ApacheCurators {
        private const val VERSION = Versions.APACHE_CURATORS

        const val RECEIPES = "org.apache.curator:curator-recipes:$VERSION"
    }

    object ApacheTika {
        private const val VERSION = Versions.APACHE_TIKA

        const val CORE = "org.apache.tika:tika-core:$VERSION"
    }

    object JAudioTagger {
        private const val VERSION = Versions.J_AUDIO_TAGGER

        const val ALL = "net.jthink:jaudiotagger:$VERSION"
    }

    object JSoup {
        private const val VERSION = Versions.JSOUP

        const val ALL = "org.jsoup:jsoup:$VERSION"
    }

    object BouncyCastle {
        private const val VERSION = Versions.BOUNCY_CASTLE

        const val BCPROV = "org.bouncycastle:bcprov-jdk15on:$VERSION"
    }

    object I2PCrypto {
        private const val VERSION = Versions.I2P_CRYPTO

        const val EDDSA = "net.i2p.crypto:eddsa:$VERSION"
    }

    object SpringSecurity {
        private const val VERSION = Versions.SPRING_SECURITY

        const val CORE = "org.springframework.security:spring-security-core:$VERSION"
    }

    object Cloudinary {
        private const val VERSION = Versions.CLOUDINARY

        const val ALL = "com.cloudinary:cloudinary-http44:$VERSION"
    }

    object Aws {
        private const val VERSION = Versions.AWS

        const val BOM = "software.amazon.awssdk:bom:$VERSION"
        const val CLOUDFRONT = "software.amazon.awssdk:cloudfront"
        const val S3 = "software.amazon.awssdk:s3"
        const val SQS = "software.amazon.awssdk:sqs"
        const val KMS = "software.amazon.awssdk:kms"
        const val SECRETS_MANAGER = "software.amazon.awssdk:secretsmanager"
        const val LAMBDA = "software.amazon.awssdk:lambda"
        const val EC2 = "software.amazon.awssdk:ec2"
        const val IMDS = "software.amazon.awssdk:imds"
    }

    object Zensum {
        const val HEALTH_CHECK = "cc.rbbl:ktor-health-check:2.0.0"
    }

    object Arweave {
        private const val ARWEAVE4S_VERSION = Versions.ARWEAVE4S
        private const val SCALA_JAVA8_COMPAT_VERSION = Versions.SCALA_JAVA8_COMPAT

        const val ARWEAVE4S = "co.upvest:arweave4s-core_2.12:$ARWEAVE4S_VERSION"
        const val SCALA_JAVA8_COMPAT = "org.scala-lang.modules:scala-java8-compat_2.12:$SCALA_JAVA8_COMPAT_VERSION"
    }

    object JUnit {
        private const val VERSION = Versions.JUNIT

        const val BOM = "org.junit:junit-bom:$VERSION"
        const val JUPITER_API = "org.junit.jupiter:junit-jupiter-api"
        const val JUPITER_PARAMS = "org.junit.jupiter:junit-jupiter-params"
        const val JUPITER_ENGINE = "org.junit.jupiter:junit-jupiter-engine"
        const val JUPITER_PLATFORM = "org.junit.platform:junit-platform-launcher"
    }

    object Cbor {
        private const val VERSION = Versions.CBOR

        const val CBOR = "io.newm:com.google.iot.cbor:$VERSION"
    }

    object Mockk {
        private const val VERSION = Versions.MOCKK

        const val MOCKK = "io.mockk:mockk:$VERSION"
    }

    object GoogleTruth {
        private const val VERSION = Versions.GOOGLE_TRUTH

        const val TRUTH = "com.google.truth:truth:$VERSION"
    }

    object Sentry {
        private const val VERSION = Versions.SENTRY

        const val CORE = "io.sentry:sentry:$VERSION"
        const val LOGBACK = "io.sentry:sentry-logback:$VERSION"
    }

    object Newm {
        private const val VERSION = Versions.KOGMIOS

        const val KOGMIOS = "io.newm:kogmios:$VERSION"
    }

    object Grpc {
        private const val VERSION = Versions.GRPC

        const val STUB = "io.grpc:grpc-stub:$VERSION"
        const val API = "io.grpc:grpc-api:$VERSION"
        const val GRPC = "io.grpc:protoc-gen-grpc-java:$VERSION"
        const val PROTOBUF = "io.grpc:grpc-protobuf:$VERSION"
        const val NETTY = "io.grpc:grpc-netty-shaded:$VERSION"
    }

    object GrpcKotlin {
        private const val VERSION = Versions.GRPC_KOTLIN

        const val STUB = "io.grpc:grpc-kotlin-stub:$VERSION"
        const val GRPCKT = "io.grpc:protoc-gen-grpc-kotlin:$VERSION:jdk8@jar"
    }

    object Protobuf {
        private const val VERSION = Versions.PROTOBUF

        const val JAVA_UTIL = "com.google.protobuf:protobuf-java-util:$VERSION"
        const val KOTLIN = "com.google.protobuf:protobuf-kotlin:$VERSION"
        const val PROTOC = "com.google.protobuf:protoc:$VERSION"
    }

    object Quartz {
        private const val VERSION = Versions.QUARTZ

        const val ALL = "org.quartz-scheduler:quartz:$VERSION"
    }

    object SSLKickstart {
        private const val VERSION = Versions.SSL_KICKSTART

        const val PEM = "io.github.hakky54:sslcontext-kickstart-for-pem:$VERSION"
        const val NETTY = "io.github.hakky54:sslcontext-kickstart-for-netty:$VERSION"
    }

    object TestContainers {
        private const val VERSION = Versions.TEST_CONTAINERS

        const val CORE = "org.testcontainers:testcontainers:$VERSION"
        const val JUINT = "org.testcontainers:junit-jupiter:$VERSION"
        const val POSTGRESQL = "org.testcontainers:postgresql:$VERSION"
    }

    object Typesafe {
        private const val VERSION = Versions.TYPESAFE
        const val CONFIG = "com.typesafe:config:$VERSION"
    }

    object QRCodeKotlin {
        private const val VERSION = Versions.QR_CODE_KOTLIN

        const val ALL = "io.github.g0dkar:qrcode-kotlin:$VERSION"
    }

    object KotlinLogging {
        private const val VERSION = Versions.KOTLIN_LOGGING
        const val ALL = "io.github.oshai:kotlin-logging:$VERSION"
    }

    object Swagger {
        private const val VERSION = Versions.SWAGGER
        const val SWAGGER_CODEGEN_GENERATORS = "io.swagger.codegen.v3:swagger-codegen-generators:$VERSION"
    }
}
