package io.newm.server.staticcontent

import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Routing

fun Routing.createStaticContentRoutes() {
    staticResources(remotePath = "/contents", basePackage = "static")
}
