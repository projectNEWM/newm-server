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
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

ktlint {
    version.set("0.42.1")
}

repositories {
    maven {
        name = "sonatypeSnapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK8)

    implementation(Dependencies.LogBack.CLASSIC)

    api(Dependencies.Grpc.STUB)
    api(Dependencies.Grpc.PROTOBUF)
    api(Dependencies.Protobuf.JAVA_UTIL)
    api(Dependencies.Protobuf.KOTLIN)
    api(Dependencies.GrpcKotlin.STUB)

    testImplementation(Dependencies.JUnit.JUPITER)
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

// work-around for protobuf plugin not registering generated sources properly.
sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/grpc",
            )
        }
        kotlin {
            srcDirs(
                "build/generated/source/proto/main/kotlin",
                "build/generated/source/proto/main/grpckt",
            )
        }
    }
}
