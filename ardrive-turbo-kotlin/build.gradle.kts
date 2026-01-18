plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
    id(Dependencies.PublishPlugin.MAVEN_PUBLISH_ID) version Dependencies.PublishPlugin.MAVEN_PUBLISH_VERSION
    id(Dependencies.PublishPlugin.SIGNING_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

version = "0.1.0-SNAPSHOT"

ktlint {
    version.set(Dependencies.KtLint.VERSION)
}

dependencies {
    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)
    implementation(Dependencies.KotlinLogging.ALL)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK9)

    implementation(Dependencies.Ktor.CLIENT_CORE)
    implementation(Dependencies.Ktor.CLIENT_CIO)
    implementation(Dependencies.Ktor.CLIENT_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.SERIALIZATION_JSON)
    implementation(Dependencies.Ktor.CLIENT_LOGGING)

    implementation(Dependencies.KotlinXSerialization.JSON)
    implementation(Dependencies.BouncyCastle.BCPROV)

    testImplementation(platform(Dependencies.JUnit.BOM))
    testImplementation(Dependencies.JUnit.JUPITER_API)
    testImplementation(Dependencies.JUnit.JUPITER_PARAMS)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_ENGINE)
    testRuntimeOnly(Dependencies.JUnit.JUPITER_PLATFORM)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Ktor.CLIENT_MOCK)
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.newm", "ardrive-turbo-kotlin", version.toString())
    pom {
        name.set("ardrive-turbo-kotlin")
        description.set("Kotlin SDK for ArDrive Turbo API")
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
