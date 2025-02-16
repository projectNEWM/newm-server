package io.newm.ktor.server.grpc

import io.grpc.BindableService
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.CommandLineConfig
import io.ktor.server.engine.EmbeddedServer

object EngineMain {
    /**
     * GRPC engine entry point
     */
    @JvmStatic
    public fun main(
        args: Array<String>,
        configure: GRPCApplicationEngine.Configuration.(appConfig: ApplicationConfig) -> Unit = {}
    ) {
        val server = createServer(args, configure)
        server.start(true)
    }

    public fun createServer(
        args: Array<String>,
        configure: GRPCApplicationEngine.Configuration.(appConfig: ApplicationConfig) -> Unit = {},
    ): EmbeddedServer<GRPCApplicationEngine, GRPCApplicationEngine.Configuration> {
        val config = CommandLineConfig(args)
        return EmbeddedServer(config.rootConfig, GRPC) {
            takeFrom(config.engineConfig)
            loadConfiguration(config.rootConfig.environment.config)
            configure.invoke(this, config.rootConfig.environment.config)
        }
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
        wait = config.propertyOrNull("wait")?.getString()?.toBooleanStrictOrNull() != false
        startEnvironment = config.propertyOrNull("startEnvironment")?.getString()?.toBooleanStrictOrNull() != false
    }
}
