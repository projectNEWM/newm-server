package io.newm.chain

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.ktor.server.application.Application
import io.newm.chain.database.initializeDatabase
import io.newm.chain.di.installDependencyInjection
import io.newm.chain.grpc.GrpcConfig
import io.newm.chain.grpc.JwtAuthorizationServerInterceptor
import io.newm.chain.logging.initializeSentry
import io.newm.shared.daemon.initializeDaemons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private var createJwtUser: String = ""

fun main(args: Array<String>) {
    // Set root log level to INFO
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    when {
        "DEBUG" in args -> {
            root.level = Level.DEBUG
        }

        "TRACE" in args -> {
            root.level = Level.TRACE
        }

        else -> {
            root.level = Level.INFO
        }
    }
    // Regular SQL logging
    (exposedLogger as Logger).level = Level.INFO
    // Detailed SQL logging
    // (exposedLogger as Logger).level = Level.DEBUG

    if ("JWT" in args) {
        createJwtUser = args[args.indexOf("JWT") + 1]
    }

    io.newm.ktor.server.grpc.EngineMain.main(args, null, GrpcConfig.init)
}

@Suppress("unused")
fun Application.module() {
    initializeSentry()
    initializeDatabase()

    installDependencyInjection()

    if (createJwtUser.isNotBlank()) {
        JwtAuthorizationServerInterceptor(
            environment.config.config("jwt")
        ).createJwtUser(createJwtUser)
        // shutdown the ktor server
        launch {
            delay(1000)
            exitProcess(0)
        }
    } else {
        initializeDaemons()
    }
}
