package io.projectnewm.server.staticcontent

import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticBasePackage
import io.ktor.server.routing.Routing

fun Routing.createStaticContentRoutes() {
    static("/contents") {
        staticBasePackage = "static"
        resources(".")
    }
}
