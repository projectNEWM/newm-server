package io.newm.server.features.doc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Routing

private val log = KotlinLogging.logger {}

/**
 * Routes that are not protected by authentication for serving the open api documentation.
 */
fun Routing.createOpenApiDocumentationRoutes() {
    try {
        swaggerUI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    } catch (e: Throwable) {
        log.error(e) { "Failed to create open api documentation routes" }
        throw e
    }
}
