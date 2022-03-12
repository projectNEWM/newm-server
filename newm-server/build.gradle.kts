plugins {
    application
    id("com.github.ben-manes.versions")
    id("com.github.johnrengelman.shadow")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

application {
    mainClass.set("io.projectnewm.server.ApplicationKt")
}

dependencies {
    implementation(project(":newm-server-common"))
    implementation(project(":newm-server-portal"))
    implementation(project(":newm-server-mobile"))

    compileOnly(Dependencies.Kotlin.REFLECTION)
    implementation(Dependencies.Kotlin.STDLIB_JDK8)

    implementation(Dependencies.Coroutines.CORE)
    implementation(Dependencies.Coroutines.JDK8)

    implementation(Dependencies.KotlinSerialization.JSON)

    implementation(Dependencies.KotlinDateTime.ALL)

    implementation(Dependencies.Ktor.SERVER_CORE)
    implementation(Dependencies.Ktor.SERVER_CIO)
    implementation(Dependencies.Ktor.SERVER_SESSIONS)
    implementation(Dependencies.Ktor.SERVER_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.SERVER_LOCATIONS)
    implementation(Dependencies.Ktor.SERVER_CALL_LOGGING)
    implementation(Dependencies.Ktor.SERVER_AUTH)
    implementation(Dependencies.Ktor.SERVER_AUTH_JWT)
    implementation(Dependencies.Ktor.SERVER_HTML_BUILDER)
    implementation(Dependencies.Ktor.CLIENT_CORE)
    implementation(Dependencies.Ktor.CLIENT_CIO)
    implementation(Dependencies.Ktor.CLIENT_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.CLIENT_SERIALIZATION)
    implementation(Dependencies.Ktor.SERIALIZATION)
    implementation(Dependencies.Ktor.SERIALIZATION_JSON)
    implementation(Dependencies.Ktor.SERVER_STATUS_PAGES)
    implementation(Dependencies.Ktor.SERVER_CORS)

    implementation(Dependencies.Koin.KTOR)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Expose.CORE)
    implementation(Dependencies.Expose.DAO)
    implementation(Dependencies.Expose.JDBC)
    implementation(Dependencies.Expose.TIME)

    implementation(Dependencies.HikariCP.ALL)

    implementation(Dependencies.PostgreSQL.ALL)

    implementation(Dependencies.ApacheCommonsEmail.ALL)

    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Ktor.SERVER_TESTS)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Koin.JUNIT)
}

tasks {
    shadowJar {
        // defaults to project.name
        // archiveBaseName.set("${project.name}-fat")

        // defaults to all, so removing this overrides the normal, non-fat jar
        archiveClassifier.set("")
    }
}
