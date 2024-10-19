package io.newm.server.curator

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.server.curator.support.CuratorPlugin
import io.newm.server.curator.support.Ec2EnsembleProvider
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getBoolean
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getString
import org.apache.curator.retry.ExponentialBackoffRetry
import software.amazon.awssdk.services.ec2.Ec2Client

fun Application.installCurator() {
    val config = environment.getConfigChild("curator")
    if (config.getBoolean("enabled")) {
        install(CuratorPlugin) {
            val ec2Client: Ec2Client by inject()
            ensembleProvider(
                Ec2EnsembleProvider(
                    ec2Client = ec2Client,
                    role = config.getString("ensembleRole"),
                    port = config.getInt("ensemblePort")
                )
            )
            retryPolicy(ExponentialBackoffRetry(config.getInt("baseSleepTime"), config.getInt("maxRetries")))
        }
    }
}
