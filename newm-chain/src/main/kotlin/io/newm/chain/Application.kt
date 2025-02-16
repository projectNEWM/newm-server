package io.newm.chain

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.newm.chain.database.initializeDatabase
import io.newm.chain.di.installDependencyInjection
import io.newm.chain.grpc.GrpcConfig
import io.newm.chain.grpc.JwtAuthorizationServerInterceptor
import io.newm.chain.logging.initializeSentry
import io.newm.shared.daemon.initializeDaemons
import java.lang.management.ManagementFactory
import java.util.jar.JarFile
import kotlin.system.exitProcess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.exposedLogger
import org.slf4j.LoggerFactory

private var createJwtUser: String = ""

private val log by lazy { KotlinLogging.logger {} }

private fun printJvmCommandLine() {
    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    val jvmArgs = runtimeMxBean.inputArguments.joinToString(" ")
    val jarPath = Application::class.java.protectionDomain.codeSource.location.path
    val jarFile = JarFile(jarPath)
    val manifest = jarFile.manifest
    val buildTime = manifest.mainAttributes.getValue("Build-Time")

    log.info { "******************************************************" }
    log.info { "JVM command line arguments: $jvmArgs" }
    log.info { "Max Heap Size: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB" }
    log.info { "Build Time: $buildTime" }
    log.info { "******************************************************" }
}

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

    printJvmCommandLine()
    io.newm.ktor.server.grpc.EngineMain
        .main(args, GrpcConfig.init)
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
