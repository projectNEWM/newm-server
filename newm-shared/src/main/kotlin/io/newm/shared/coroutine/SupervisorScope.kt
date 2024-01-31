package io.newm.shared.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

interface SupervisorScope : CoroutineScope {
    val log: Logger

    override val coroutineContext: CoroutineContext
        get() {
            val key = javaClass.canonicalName
            return coroutineContexts.computeIfAbsent(key) {
                SupervisorJob() + Dispatchers.IO +
                    CoroutineExceptionHandler { _, throwable ->
                        if (throwable !is CancellationException) {
                            log.error("Unhandled Coroutine Exception!", throwable)
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
