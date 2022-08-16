package io.newm.chain.daemon

import io.ktor.server.application.*
import io.newm.chain.logging.captureToSentry
import io.newm.kogmios.LocalChainSyncClient
import io.newm.kogmios.createLocalChainSyncClient
import io.newm.server.di.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.IOException

class BlockDaemon(
    private val environment: ApplicationEnvironment
) : Daemon {
    override val log: Logger by inject { parametersOf("BlockController") }

    override fun start() {
        log.info("starting...")
        launch {
            while (true) {
                try {
                    val client = connectBlockchain()
                    syncBlockchain(client)
                } catch (e: Throwable) {
                    log.error("Error syncing blockchain!", e)
                    e.captureToSentry()
                    log.info("Wait 10 seconds to retry...")
                    delay(RETRY_DELAY_MILLIS)
                }
            }
        }
        log.info("startup complete.")
    }

    override fun shutdown() {
        log.info("shutdown complete.")
    }

    private suspend fun connectBlockchain(): LocalChainSyncClient {
        val server = environment.config.property("ogmios.server").getString()
        val port = environment.config.property("ogmios.port").getString().toInt()

        val client = createLocalChainSyncClient(
            websocketHost = server,
            websocketPort = port,
            ogmiosCompact = true
        )
        val connectResult = client.connect()
        if (!connectResult) {
            throw IOException("client.connect() was false!")
        }
        if (!client.isConnected) {
            throw IOException("client.isConnected was false!")
        }

        return client
    }

    private suspend fun syncBlockchain(client: LocalChainSyncClient) {
        // TODO: Sync the blockchain
    }

    companion object {
        private const val RETRY_DELAY_MILLIS = 10_000L
    }
}
