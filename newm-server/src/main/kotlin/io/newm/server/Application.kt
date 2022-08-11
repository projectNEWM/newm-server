package io.newm.server

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.newm.server.auth.createAuthenticationRoutes
import io.newm.server.auth.installAuthentication
import io.newm.server.content.installContentNegotiation
import io.newm.server.cors.installCORS
import io.newm.server.database.initializeDatabase
import io.newm.server.di.installDependencyInjection
import io.newm.server.features.cloudinary.createCloudinaryRoutes
import io.newm.server.features.playlist.createPlaylistRoutes
import io.newm.server.features.song.createSongRoutes
import io.newm.server.features.user.createUserRoutes
import io.newm.server.logging.initializeSentry
import io.newm.server.logging.installCallLogging
import io.newm.server.staticcontent.createStaticContentRoutes
import io.newm.server.statuspages.installStatusPages

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
        createStaticContentRoutes()
        createAuthenticationRoutes()
        createUserRoutes()
        createSongRoutes()
        createPlaylistRoutes()
        createCloudinaryRoutes()
    }
}
