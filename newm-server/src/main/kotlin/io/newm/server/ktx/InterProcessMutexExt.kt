package io.newm.server.ktx

import com.github.benmanes.caffeine.cache.Caffeine
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.apache.curator.framework.recipes.locks.InterProcessMutex

// InterProcessMutext does not play nice with coroutines if the block() resumes on a different thread
// This extension function ensures that the block() is executed on the same thread as the lock and
// releases the lock after the block is executed on the same thread.

@OptIn(ExperimentalCoroutinesApi::class)
private val singleThreadContextCache = Caffeine
    .newBuilder()
    .build<String, CoroutineContext> { name ->
        @OptIn(DelicateCoroutinesApi::class)
        newSingleThreadContext(name)
    }

suspend fun <T> InterProcessMutex.withLock(
    lockName: String,
    block: suspend () -> T
): T =
    withContext(singleThreadContextCache.get(lockName)) {
        // acquire() with throw and exit early if we couldn't acquire the lock
        acquire()
        try {
            block()
        } finally {
            release()
        }
    }
