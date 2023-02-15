package io.newm.objectpool

import java.io.Closeable

interface ObjectPool<T : Any> : Closeable {
    /**
     * Pool capacity limit
     */
    val capacity: Int

    /**
     * Borrow an instance. Could be a recycled instance or a new one.
     */
    suspend fun borrow(): T

    /**
     * Recycle an instance so it can be used again
     */
    suspend fun recycle(instance: T)

    /**
     * Dispose of all items in the pool.
     */
    fun dispose()

    /**
     * Allows you to pool.use {...} and have things cleaned up still.
     */
    override fun close() {
        dispose()
    }
}

/**
 * Borrows [T] from the pool, invokes [block] on it and then recycles
 */
suspend inline fun <T : Any, R> ObjectPool<T>.useInstance(block: (T) -> R): R {
    val instance = borrow()
    try {
        return block(instance)
    } finally {
        recycle(instance)
    }
}
