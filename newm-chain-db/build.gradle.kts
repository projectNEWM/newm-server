plugins {
    `java-library`
    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
    kotlin("plugin.serialization")
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

    implementation(Dependencies.KotlinXSerialization.JSON)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(Dependencies.Newm.KOGMIOS)

    implementation(Dependencies.Exposed.CORE)
    implementation(Dependencies.Exposed.DAO)
    implementation(Dependencies.Exposed.JDBC)
    implementation(Dependencies.Exposed.TIME)

    implementation(Dependencies.HikariCP.ALL)
    implementation(Dependencies.PostgreSQL.ALL)
    implementation(Dependencies.FlywayDB.ALL)
    implementation(Dependencies.Caffeine.ALL)

    implementation(Dependencies.LibSodiumJNA.SODIUM)
    implementation(Dependencies.Cbor.CBOR)
    implementation(Dependencies.ApacheCommonsCodec.ALL)
    implementation(Dependencies.BouncyCastle.BCPROV)
    implementation(Dependencies.SpringSecurity.CORE) {
        // We don't care about other spring stuff.
        // We just like using Encryptors.stronger
        exclude(group = "org.springframework")
    }

    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
    testImplementation(Dependencies.TestContainers.POSTGRESQL)
}
