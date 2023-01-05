package io.newm.ktor.server.grpc

import io.grpc.BindableService
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.commandLineEnvironment

object EngineMain {
    /**
     * GRPC engine entry point
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val applicationEnvironment = commandLineEnvironment(args)
        val engine = GRPCApplicationEngine(applicationEnvironment) { loadConfiguration(applicationEnvironment.config) }
        val gracePeriod =
            engine.environment.config.propertyOrNull("ktor.deployment.shutdownGracePeriod")?.getString()?.toLong()
                ?: 50
        val timeout =
            engine.environment.config.propertyOrNull("ktor.deployment.shutdownTimeout")?.getString()?.toLong()
                ?: 5000
        engine.addShutdownHook {
            engine.stop(gracePeriod, timeout)
        }
        engine.start(true)
    }

    private fun GRPCApplicationEngine.Configuration.loadConfiguration(config: ApplicationConfig) {
        val grpcConfig = config.config("grpc")
        loadGrpcConfiguration(grpcConfig)
    }

    private fun GRPCApplicationEngine.Configuration.loadGrpcConfiguration(config: ApplicationConfig) {
        port = config.property("port").getString().toInt()
        configFileServerConfigurer = {
            config.propertyOrNull("services")?.getList()?.let { services ->
                services.forEach { serviceClassString ->
                    val service = Class.forName(serviceClassString).getConstructor().newInstance() as BindableService
                    addService(service)
                }
            }
        }
    }

}