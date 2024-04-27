package io.newm.server.curator

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.server.curator.support.CuratorPlugin
import io.newm.server.curator.support.FileEnsembleProvider
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getString
import org.apache.curator.retry.ExponentialBackoffRetry

fun Application.installCurator() {
    val config = environment.getConfigChild("curator")
    install(CuratorPlugin) {
        ensembleProvider(FileEnsembleProvider(config.getString("connectStringFilePath")))
        retryPolicy(ExponentialBackoffRetry(config.getInt("baseSleepTime"), config.getInt("maxRetries")))
    }
}
