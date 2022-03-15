package io.projectnewm.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.projectnewm.server.auth.createAuthenticationRoutes
import io.projectnewm.server.auth.installAuthentication
import io.projectnewm.server.content.installContentNegotiation
import io.projectnewm.server.cors.installCORS
import io.projectnewm.server.database.initializeDatabase
import io.projectnewm.server.debug.createDebugRoutes
import io.projectnewm.server.koin.installDependencyInjection
import io.projectnewm.server.logging.installCallLogging
import io.projectnewm.server.sessions.installSessions
import io.projectnewm.server.statuspages.installStatusPages
import io.projectnewm.server.user.createUserRoutes

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.mainModule() {
    installCallLogging()
    installDependencyInjection()
    installContentNegotiation()
    installSessions()
    installAuthentication()
    installStatusPages()
    installCORS()

    initializeDatabase()

    routing {
        createAuthenticationRoutes()
        createUserRoutes()
        createDebugRoutes()
    }
}
