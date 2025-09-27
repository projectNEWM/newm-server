package io.newm.shared.application

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import java.lang.management.ManagementFactory
import java.util.jar.JarFile

/**
 * Logs common JVM / build information at application startup.
 * Can be reused by any service (HTTP, gRPC, etc.) for consistent startup diagnostics.
 *
 * @param log Optional logger. If not provided a default logger will be used.
 */
fun printJvmCommandLine(log: KLogger = KotlinLogging.logger {}) {
    try {
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val jvmArgs = runtimeMxBean.inputArguments.joinToString(" ")
        val jarPath = Application::class.java.protectionDomain.codeSource.location.path
        val buildTime = JarFile(jarPath).use { jarFile ->
            jarFile.manifest.mainAttributes.getValue("Build-Time")
        }

        log.info { "******************************************************" }
        log.info { "JVM command line arguments: $jvmArgs" }
        log.info { "Max Heap Size: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB" }
        log.info { "Build Time: $buildTime" }
        log.info { "******************************************************" }
    } catch (e: Exception) {
        // Fail-safe: never let a logging helper prevent startup
        log.warn(e) { "Unable to log JVM startup information." }
    }
}
