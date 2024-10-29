package io.newm.server.logging

import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.slf4j.MDC
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils

fun Application.initializeLogging() {
    val instanceId = try {
        EC2MetadataUtils.getInstanceId()
    } catch (e: Throwable) {
        log.error("Failed to get instanceId from EC2MetadataUtils", e)
        "instanceId-unknown"
    }
    MDC.put("instanceId", instanceId)
}
