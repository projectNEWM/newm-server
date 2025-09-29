package io.newm.server.curator

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.server.curator.support.CuratorPlugin
import io.newm.shared.ktx.getBoolean
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getString
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider
import org.apache.curator.retry.ExponentialBackoffRetry

fun Application.installCurator() {
    val log = KotlinLogging.logger { }
    val config = environment.getConfigChild("curator")
    if (config.getBoolean("enabled")) {
        install(CuratorPlugin) {
            ensembleProvider(
                FixedEnsembleProvider(
                    config.getString("connectionString").also { connectionString ->
                        log.info { "Using Zookeeper connection string: $connectionString" }
                        // do a dns lookup of the connection string.
                        val address = java.net.InetAddress.getByName(connectionString.split(":").first())
                        log.info { "Using Zookeeper address: $address" }
                    },
                    false
                ),
            )
            retryPolicy(ExponentialBackoffRetry(config.getInt("baseSleepTime"), config.getInt("maxRetries")))
        }
    }
}
