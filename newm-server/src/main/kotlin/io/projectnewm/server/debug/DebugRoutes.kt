package io.projectnewm.server.debug

import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticBasePackage
import io.ktor.server.routing.Routing

// Routes debug & testing during development
fun Routing.createDebugRoutes() {
    static("/") {
        staticBasePackage = "static"
        resources(".")
        defaultResource("index.html")
    }
}
