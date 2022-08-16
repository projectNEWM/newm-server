package io.newm.chain.daemon

import io.ktor.server.application.*
import org.koin.ktor.ext.getKoin

fun Application.initializeDaemons() {

    val daemons = getKoin().getAll<Daemon>()

    // Add shutdown hook
    environment.monitor.subscribe(ApplicationStopPreparing) {
        // Shutdown the Daemons
        daemons.forEach { controller -> controller.shutdown() }
    }

    daemons.forEach { it.start() }
}
