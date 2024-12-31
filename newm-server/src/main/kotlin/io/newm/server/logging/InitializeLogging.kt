package io.newm.server.logging

import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.slf4j.MDC
import software.amazon.awssdk.imds.Ec2MetadataClient

fun Application.initializeLogging() {
    val instanceId = try {
        Ec2MetadataClient.create().use { ec2MetadataClient ->
            ec2MetadataClient.get("/latest/meta-data/instance-id").asString()
        }
    } catch (e: Throwable) {
        log.error("Failed to get instanceId from EC2MetadataUtils", e)
        "instanceId-unknown"
    }
    MDC.put("instanceId", instanceId)
}
