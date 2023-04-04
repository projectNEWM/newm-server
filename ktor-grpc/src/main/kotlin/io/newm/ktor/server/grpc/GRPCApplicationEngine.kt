package io.newm.ktor.server.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.ktor.events.raiseCatching
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ServerReady
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.BaseApplicationEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

object GRPC : ApplicationEngineFactory<GRPCApplicationEngine, GRPCApplicationEngine.Configuration> {
    override fun create(
        environment: ApplicationEngineEnvironment,
        configure: GRPCApplicationEngine.Configuration.() -> Unit
    ): GRPCApplicationEngine {
        return GRPCApplicationEngine(environment, configure)
    }
}

class GRPCApplicationEngine(
    environment: ApplicationEngineEnvironment,
    configure: Configuration.() -> Unit = {}
) : BaseApplicationEngine(environment) {

    private val log by lazy { LoggerFactory.getLogger("GRPCApplicationEngine") }

    class Configuration : BaseApplicationEngine.Configuration() {
        var port: Int = 8080
        var wait: Boolean = false
        var serverConfigurer: ServerBuilder<*>.() -> Unit = {}
        internal var configFileServerConfigurer: ServerBuilder<*>.() -> Unit = {}
        var startEnvironment: Boolean = true
    }

    private val configuration = Configuration().apply(configure)

    private val engineDispatcher = Dispatchers.IO
    private val userDispatcher = Dispatchers.IO

    private val startupJob: CompletableDeferred<Unit> = CompletableDeferred()
    private val stopRequest: CompletableJob = Job()

    private var serverJob: AtomicReference<Job> = AtomicReference(Job())

    private lateinit var server: Server

    init {
        serverJob.set(initServerJob())
        serverJob.get().invokeOnCompletion { cause ->
            cause?.let { stopRequest.completeExceptionally(cause) }
            cause?.let { startupJob.completeExceptionally(cause) }
            environment.stop()
        }
    }

    override fun start(wait: Boolean): ApplicationEngine {
        serverJob.get().start()

        runBlocking {
            startupJob.await()
            environment.monitor.raiseCatching(ServerReady, environment, environment.log)

            if (wait || configuration.wait) {
                serverJob.get().join()
            }
        }

        return this
    }

    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        stopRequest.complete()

        runBlocking {
            val result = withTimeoutOrNull(gracePeriodMillis) {
                serverJob.get().join()
                true
            }

            if (result == null) {
                // timeout
                serverJob.get().cancel()

                val forceShutdown = withTimeoutOrNull(timeoutMillis - gracePeriodMillis) {
                    serverJob.get().join()
                    false
                } ?: true

                if (forceShutdown) {
                    environment.stop()
                    if (::server.isInitialized && !server.isTerminated) {
                        server.shutdownNow()
                    }
                }
            }
        }
    }

    private fun initServerJob(): Job {
        val environment = environment
        val userDispatcher = userDispatcher
        val stopRequest = stopRequest
        val startupJob = startupJob

        return CoroutineScope(
            environment.parentCoroutineContext + engineDispatcher
        ).launch(start = CoroutineStart.LAZY) {
            try {
                if (configuration.startEnvironment) {
                    withContext(userDispatcher) {
                        environment.start()
                    }
                }

                // Start the GRPC server
                server = ServerBuilder
                    .forPort(configuration.port)
                    .apply(configuration.configFileServerConfigurer)
                    .apply(configuration.serverConfigurer)
                    .build()
                    .start()

                log.info("Started GRPC Server on port: ${configuration.port}")
                server.services.forEach { service ->
                    log.info("Service Ready: ${service.serviceDescriptor.name}")
                }
            } catch (cause: Throwable) {
                log.error("Error starting GRPCApplicationEngine!", cause)
                stopRequest.completeExceptionally(cause)
                startupJob.completeExceptionally(cause)
                throw cause
            }

            startupJob.complete(Unit)
            stopRequest.join()

            withContext(userDispatcher) {
                environment.monitor.raise(ApplicationStopPreparing, environment)
            }

            // We've gotten a stopRequest. Shutdown and wait here for termination.
            server.shutdown().awaitTermination()
        }
    }
}
