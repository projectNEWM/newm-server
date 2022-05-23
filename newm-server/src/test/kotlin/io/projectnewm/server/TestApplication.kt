package io.projectnewm.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.projectnewm.server.auth.createAuthenticationRoutes
import io.projectnewm.server.auth.installFakeAuthentication
import io.projectnewm.server.content.installContentNegotiation
import io.projectnewm.server.di.installDependencyInjection
import io.projectnewm.server.features.cloudinary.createCloudinaryRoutes
import io.projectnewm.server.features.playlist.createPlaylistRoutes
import io.projectnewm.server.features.song.createSongRoutes
import io.projectnewm.server.features.user.createUserRoutes
import io.projectnewm.server.staticcontent.createStaticContentRoutes
import io.projectnewm.server.statuspages.installStatusPages

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.testModule() {
    installDependencyInjection()
    installContentNegotiation()
    installFakeAuthentication()
    installStatusPages()

    routing {
        createStaticContentRoutes()
        createAuthenticationRoutes()
        createUserRoutes()
        createSongRoutes()
        createPlaylistRoutes()
        createCloudinaryRoutes()
    }
}
