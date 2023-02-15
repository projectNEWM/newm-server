package io.newm.chain.grpc

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.pool.DefaultPool
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.TxSubmitClient
import io.newm.kogmios.createTxSubmitClient
import io.newm.kogmios.protocols.model.InstantQueryResult
import io.newm.server.di.inject
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger

class TxSubmitClientPool(
    capacity: Int
) : DefaultPool<TxSubmitClient>(capacity) {
    private val log: Logger by inject { parametersOf("OgmiosClientPool") }

    private val environment: ApplicationEnvironment by inject()
    private val ogmiosConfig by lazy { environment.config.config("ogmios") }
    private val websocketHost: String by lazy {
        ogmiosConfig.property("server").getString()
    }
    private val websocketPort: Int by lazy {
        ogmiosConfig.property("port").getString().toInt()
    }
    private val secure: Boolean by lazy {
        ogmiosConfig.property("secure").getString().toBoolean()
    }

    override fun produceInstance(): TxSubmitClient {
        // FIXME: This is a code smell that can cause the calling thread to block.
        // We should replace the whole object pooling setup with our own that uses
        // coroutines natively in the future.
        return runBlocking {
            try {
                val client = createTxSubmitClient(websocketHost, websocketPort, secure)
                val connectResult = client.connect()
                require(connectResult) { "Could not connect to ogmios!" }
                require(client.isConnected) { "Ogmios not connected!" }
                client
            } catch (e: Throwable) {
                log.error("Could not produceInstance()!", e)
                throw e
            }
        }
    }

    override fun validateInstance(instance: TxSubmitClient) {
        // FIXME: This is a code smell that can cause the calling thread to block.
        // We should replace the whole object pooling setup with our own that uses
        // coroutines natively in the future.
        runBlocking {
            try {
                require(instance.isConnected) { "Ogmios not connected!" }

                // Query the SystemStart value to validate the instance
                val result = (instance as StateQueryClient).systemStart().result as? InstantQueryResult
                requireNotNull(result) { "Could not validate systemStart!" }
            } catch (e: Throwable) {
                log.error("Could not validateInstance()!", e)
                throw e
            }
        }
        super.validateInstance(instance)
    }

    override fun disposeInstance(instance: TxSubmitClient) {
        // FIXME: This is a code smell that can cause the calling thread to block.
        // We should replace the whole object pooling setup with our own that uses
        // coroutines natively in the future.
        runBlocking {
            try {
                instance.shutdown()
            } catch (e: Throwable) {
                log.error("Could not disposeInstance()!", e)
                throw e
            }
        }
        super.disposeInstance(instance)
    }
}
