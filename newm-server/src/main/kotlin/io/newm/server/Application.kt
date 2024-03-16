package io.newm.server

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.newm.server.auth.createAuthenticationRoutes
import io.newm.server.auth.installAuthentication
import io.newm.server.content.installContentNegotiation
import io.newm.server.cors.installCORS
import io.newm.server.database.initializeDatabase
import io.newm.server.di.installDependencyInjection
import io.newm.server.features.cardano.createCardanoRoutes
import io.newm.server.features.cloudinary.createCloudinaryRoutes
import io.newm.server.features.collaboration.createCollaborationRoutes
import io.newm.server.features.distribution.createDistributionRoutes
import io.newm.server.features.earnings.createEarningsRoutes
import io.newm.server.features.idenfy.createIdenfyRoutes
import io.newm.server.features.playlist.createPlaylistRoutes
import io.newm.server.features.walletconnection.createWalletConnectionRoutes
import io.newm.server.features.song.createSongRoutes
import io.newm.server.features.user.createUserRoutes
import io.newm.server.forwarder.installForwarder
import io.newm.server.health.installHealthCheck
import io.newm.server.logging.initializeSentry
import io.newm.server.logging.installCallLogging
import io.newm.server.staticcontent.createStaticContentRoutes
import io.newm.server.statuspages.installStatusPages
import io.newm.shared.daemon.initializeDaemons

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    initializeSentry()
    installDependencyInjection()
    initializeDatabase()

    installCallLogging()
    installContentNegotiation()
    installAuthentication()
    installStatusPages()
    installCORS()
    installForwarder()
    installHealthCheck()

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
        createDistributionRoutes()
        createEarningsRoutes()
        createWalletConnectionRoutes()
    }

    initializeDaemons()
}
