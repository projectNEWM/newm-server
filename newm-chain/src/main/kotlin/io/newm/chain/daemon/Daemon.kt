package io.newm.chain.daemon

import io.newm.chain.logging.captureToSentry
import kotlinx.coroutines.*
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

interface Daemon : CoroutineScope {
    val log: Logger

    fun start()
    fun shutdown()

    override val coroutineContext: CoroutineContext
        get() {
            val key = javaClass.canonicalName
            return coroutineContexts.computeIfAbsent(key) {
                SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                    if (throwable !is CancellationException) {
                        log.error("Unhandled Coroutine Exception!", throwable)
                        throwable.captureToSentry()
                        Thread.sleep(5000)
                        exitProcess(1)
                    } else {
                        log.warn("CancellationException!", throwable)
                    }
                }
            }
        }

    companion object {
        private val coroutineContexts = ConcurrentHashMap<String, CoroutineContext>()
    }
}
