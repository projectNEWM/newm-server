package io.projectnewm.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.projectnewm.server.auth.createAuthenticationRoutes
import io.projectnewm.server.auth.installAuthentication
import io.projectnewm.server.content.installContentNegotiation
import io.projectnewm.server.cors.installCORS
import io.projectnewm.server.database.initializeDatabase
import io.projectnewm.server.di.installDependencyInjection
import io.projectnewm.server.features.song.createSongRoutes
import io.projectnewm.server.features.user.createUserRoutes
import io.projectnewm.server.logging.initializeSentry
import io.projectnewm.server.logging.installCallLogging
import io.projectnewm.server.statuspages.installStatusPages

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    initializeSentry()
    initializeDatabase()

    installCallLogging()
    installDependencyInjection()
    installContentNegotiation()
    installAuthentication()
    installStatusPages()
    installCORS()

    routing {
        createAuthenticationRoutes()
        createUserRoutes()
        createSongRoutes()
    }
}
