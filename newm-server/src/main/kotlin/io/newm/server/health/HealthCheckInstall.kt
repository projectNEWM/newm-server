package io.newm.server.health

import cc.rbbl.ktor_health_check.Health
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.install
import io.newm.server.aws.s3.doesBucketExist
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import software.amazon.awssdk.services.s3.S3Client

// livez is the preferred endpoint over the deprecated healthz. We implement both for good measure.
private const val HEALTHZ_CHECK_URL = "healthz"
private const val LIVEZ_CHECK_URL = "livez"

fun Health.Configuration.healthtechChecks() {
    listOf(HEALTHZ_CHECK_URL, LIVEZ_CHECK_URL).forEach { endpoint ->
        customCheck(endpoint, "status") {
            true
        }
    }
}

fun Application.installHealthCheck() {
//    val hikariDataSource: HikariDataSource by inject()
    val environment: ApplicationEnvironment by inject()
    val s3: S3Client by inject()

    install(Health) {
        healthtechChecks()

//        readyCheck("database") {
//            hikariDataSource.connection.isValid(5)
//        }

        readyCheck("aws.s3") {
            val bucketName = environment.getConfigString("aws.s3.agreement.bucketName")
            s3.doesBucketExist(bucketName)
        }
    }
}
