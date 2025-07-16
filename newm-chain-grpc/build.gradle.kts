import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    id(Dependencies.ProtobufPlugin.ID)
    id(Dependencies.PublishPlugin.MAVEN_PUBLISH_ID) version Dependencies.PublishPlugin.MAVEN_PUBLISH_VERSION
    id(Dependencies.PublishPlugin.SIGNING_ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

ktlint {
    version.set(Dependencies.KtLint.VERSION)
    filter {
        exclude { entry ->
            entry.file.toString().contains("generated")
        }
    }
}

dependencies {
    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK9)

    implementation(Dependencies.LogBack.CLASSIC)

    api(Dependencies.Grpc.STUB)
    api(Dependencies.Grpc.PROTOBUF)
    api(Dependencies.Protobuf.JAVA_UTIL)
    api(Dependencies.Protobuf.KOTLIN)
    api(Dependencies.GrpcKotlin.STUB)

    testImplementation(platform(Dependencies.JUnit.BOM))
    testImplementation(Dependencies.JUnit.JUPITER_API)
    testImplementation(Dependencies.JUnit.JUPITER_PARAMS)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_ENGINE)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_PLATFORM)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
}

protobuf {
    protoc {
        artifact = Dependencies.Protobuf.PROTOC
    }
    plugins {
        id("grpc") {
            artifact = Dependencies.Grpc.GRPC
        }
        id("grpckt") {
            artifact = Dependencies.GrpcKotlin.GRPCKT
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.newm.server", "newm-chain-grpc", version.toString())
    pom {
        name.set("newm-chain-grpc")
        description.set("NEWM Chain gRPC")
        url.set("https://github.com/projectNEWM/newm-server")
        licenses {
            license {
                name.set("Apache 2.0")
                url.set("https://github.com/projectNEWM/newm-server/blob/master/LICENSE")
            }
        }
        developers {
            developer {
                id.set("AndrewWestberg")
                name.set("Andrew Westberg")
                email.set("andrewwestberg@gmail.com")
                organization.set("NEWM")
                organizationUrl.set("https://newm.io")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/projectNEWM/newm-server.git")
            developerConnection.set("scm:git:ssh://github.com/projectNEWM/newm-server.git")
            url.set("https://github.com/projectNEWM/newm-server")
        }
    }
}
