package io.newm.shared.daemon

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import org.koin.ktor.ext.getKoin

fun Application.initializeDaemons() {
    val daemons = getKoin().getAll<Daemon>()

    // Add shutdown hook
    monitor.subscribe(ApplicationStopPreparing) {
        // Shutdown the Daemons
        daemons.forEach { daemon -> daemon.shutdown() }
    }

    daemons.forEach { it.start() }
}
