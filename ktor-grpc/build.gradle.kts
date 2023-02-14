import org.hibernate.build.publish.auth.maven.MavenRepoAuthPlugin

plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    id(Dependencies.PublishPlugin.MAVEN_PUBLISH_ID)
    id(Dependencies.PublishPlugin.SIGNING_ID)
    id(Dependencies.PublishPlugin.MAVEN_REPO_AUTH_ID) version Dependencies.PublishPlugin.MAVEN_REPO_AUTH_VERSION apply false
}

if (!project.hasProperty("isGithubActions")) {
    // only use this plugin if we're running locally, not on github.
    apply<MavenRepoAuthPlugin>()
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

version = "0.2.0-SNAPSHOT"

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

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_HOST_COMMON)

    implementation(Dependencies.Grpc.API)

    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
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
        from("$buildDir/javadoc")
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
                artifactId = "ktor-grpc"

                name.set("ktor-grpc")
                description.set("gRPC Application Engine for Ktor")
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
