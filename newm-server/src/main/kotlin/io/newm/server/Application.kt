package io.newm.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.newm.server.auth.createAuthenticationRoutes
import io.newm.server.auth.installAuthentication
import io.newm.server.content.installContentNegotiation
import io.newm.server.cors.installCORS
import io.newm.server.curator.installCurator
import io.newm.server.database.initializeDatabase
import io.newm.server.di.installDependencyInjection
import io.newm.server.features.cardano.createCardanoRoutes
import io.newm.server.features.clientconfig.createClientConfigRoutes
import io.newm.server.features.cloudinary.createCloudinaryRoutes
import io.newm.server.features.collaboration.createCollaborationRoutes
import io.newm.server.features.distribution.createDistributionRoutes
import io.newm.server.features.doc.createOpenApiDocumentationRoutes
import io.newm.server.features.earnings.createEarningsRoutes
import io.newm.server.features.idenfy.createIdenfyRoutes
import io.newm.server.features.marketplace.createMarketplaceRoutes
import io.newm.server.features.playlist.createPlaylistRoutes
import io.newm.server.features.song.createSongRoutes
import io.newm.server.features.user.createUserRoutes
import io.newm.server.features.walletconnection.createWalletConnectionRoutes
import io.newm.server.forwarder.installForwarder
import io.newm.server.health.installHealthCheck
import io.newm.server.logging.initializeSentry
import io.newm.server.logging.installCallLogging
import io.newm.server.staticcontent.createStaticContentRoutes
import io.newm.server.statuspages.installStatusPages
import io.newm.shared.daemon.initializeDaemons
import java.lang.management.ManagementFactory

private val log = KotlinLogging.logger {}

private fun printJvmCommandLine() {
    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    val jvmArgs = runtimeMxBean.inputArguments.joinToString(" ")
    log.info { "******************************************************" }
    log.info { "JVM command line arguments: $jvmArgs" }
    log.info { "******************************************************" }
}

fun main(args: Array<String>) {
    printJvmCommandLine()
    io.ktor.server.cio.EngineMain
        .main(args)
}

@Suppress("unused")
fun Application.module() {
    initializeSentry()
    installDependencyInjection()
    initializeDatabase()
    installCurator()

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
        createWalletConnectionRoutes()
        createMarketplaceRoutes()
        createClientConfigRoutes()
        createOpenApiDocumentationRoutes()
        createEarningsRoutes()
    }

    initializeDaemons()
}
