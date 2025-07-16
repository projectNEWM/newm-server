plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    id(Dependencies.PublishPlugin.MAVEN_PUBLISH_ID) version Dependencies.PublishPlugin.MAVEN_PUBLISH_VERSION
    id(Dependencies.PublishPlugin.SIGNING_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

version = "0.4.1-SNAPSHOT"

ktlint {
    version.set(Dependencies.KtLint.VERSION)
}

dependencies {
    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK9)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_HOST_COMMON)

    implementation(Dependencies.Grpc.API)

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

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.newm.server", "ktor-grpc", version.toString())
    pom {
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
