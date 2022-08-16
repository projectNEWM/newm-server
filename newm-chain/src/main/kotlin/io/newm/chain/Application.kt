package io.newm.chain

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.ktor.server.application.*
import io.newm.chain.daemon.initializeDaemons
import io.newm.chain.database.initializeDatabase
import io.newm.chain.di.installDependencyInjection
import io.newm.chain.logging.initializeSentry
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    // Set root log level to INFO
    val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    when {
        args.contains("DEBUG") -> {
            root.level = Level.DEBUG
        }

        args.contains("TRACE") -> {
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

    io.ktor.server.cio.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    initializeSentry()
    initializeDatabase()

    installDependencyInjection()

    initializeDaemons()
}
