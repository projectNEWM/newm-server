plugins {
    `java-library`
    id(Dependencies.VersionsPlugin.ID)
    id(Dependencies.KtlintPlugin.ID)
    kotlin(Dependencies.KotlinPlugin.JVM_ID)
    kotlin(Dependencies.KotlinPlugin.SERIALIZATION_ID)
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

ktlint {
    version.set(Dependencies.KtLint.VERSION)
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

    implementation(Dependencies.KotlinXSerialization.JSON)

    implementation(Dependencies.LogBack.CLASSIC)

    implementation(project(":newm-chain-util"))

    implementation(Dependencies.Newm.KOGMIOS)

    implementation(Dependencies.Exposed.CORE)
    implementation(Dependencies.Exposed.DAO)
    implementation(Dependencies.Exposed.JDBC)
    implementation(Dependencies.Exposed.TIME)

    implementation(Dependencies.HikariCP.ALL)
    implementation(Dependencies.PostgreSQL.ALL)
    implementation(Dependencies.FlywayDB.CORE)
    implementation(Dependencies.Caffeine.ALL)

    implementation(Dependencies.Cbor.CBOR)
    implementation(Dependencies.ApacheCommonsCodec.ALL)
    implementation(Dependencies.BouncyCastle.BCPROV)

    testImplementation(Dependencies.JUnit.JUPITER)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.TestContainers.CORE)
    testImplementation(Dependencies.TestContainers.JUINT)
    testImplementation(Dependencies.TestContainers.POSTGRESQL)
}
