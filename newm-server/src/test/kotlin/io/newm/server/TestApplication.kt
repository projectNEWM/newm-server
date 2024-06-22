package io.newm.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.newm.server.auth.createAuthenticationRoutes
import io.newm.server.auth.installFakeAuthentication
import io.newm.server.content.installContentNegotiation
import io.newm.server.di.installDependencyInjection
import io.newm.server.features.cardano.createCardanoRoutes
import io.newm.server.features.cloudinary.createCloudinaryRoutes
import io.newm.server.features.collaboration.createCollaborationRoutes
import io.newm.server.features.idenfy.createIdenfyFakeServerRoutes
import io.newm.server.features.idenfy.createIdenfyRoutes
import io.newm.server.features.marketplace.createMarketplaceRoutes
import io.newm.server.features.playlist.createPlaylistRoutes
import io.newm.server.features.song.createSongRoutes
import io.newm.server.features.user.createUserRoutes
import io.newm.server.features.walletconnection.createWalletConnectionRoutes
import io.newm.server.staticcontent.createStaticContentRoutes
import io.newm.server.statuspages.installStatusPages

fun main(args: Array<String>) =
    io.ktor.server.cio.EngineMain
        .main(args)

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
        createCardanoRoutes()
        createSongRoutes()
        createCollaborationRoutes()
        createPlaylistRoutes()
        createCloudinaryRoutes()
        createIdenfyRoutes()
        createIdenfyFakeServerRoutes()
        createWalletConnectionRoutes()
        createMarketplaceRoutes()
    }
}
