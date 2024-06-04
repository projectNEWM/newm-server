import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.hibernate.build.publish.auth.maven.MavenRepoAuthPlugin

plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    id(Dependencies.ProtobufPlugin.ID)
    id(Dependencies.PublishPlugin.MAVEN_PUBLISH_ID)
    id(Dependencies.PublishPlugin.SIGNING_ID)
    id(Dependencies.PublishPlugin.MAVEN_REPO_AUTH_ID) version Dependencies.PublishPlugin.MAVEN_REPO_AUTH_VERSION apply false
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

if (!project.hasProperty("isGithubActions")) {
    // only use this plugin if we're running locally, not on github.
    apply<MavenRepoAuthPlugin>()
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
    implementation(Dependencies.Coroutines.JDK9)

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

tasks {

    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        dependsOn("classes")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        dependsOn("javadoc")
        from("${layout.buildDirectory}/javadoc")
    }

    artifacts {
        archives(javadocJar)
        archives(sourcesJar)
    }

    assemble {
        dependsOn("sourcesJar", "javadocJar")
    }
}

publishing {
    repositories {
        maven {
            name = "ossrh"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            if (project.hasProperty("release")) {
                setUrl(releasesRepoUrl)
            } else {
                setUrl(snapshotsRepoUrl)
            }
        }
    }
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                groupId = "io.newm.server"
                artifactId = "newm-chain-grpc"

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
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenKotlin"])
}
