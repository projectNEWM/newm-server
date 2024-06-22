package io.newm.objectpool

import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray

private const val MULTIPLIER = 4
private const val ATTEMPTS = 8
private const val GOLDEN_FRACTION = 2654435769.toInt()
private const val MAX_CAPACITY = Int.MAX_VALUE / MULTIPLIER

/**
 * Default implementation
 */
abstract class DefaultPool<T : Any>(
    final override val capacity: Int
) : ObjectPool<T> {
    init {
        require(capacity > 0) { "capacity must be > 0, but it was $capacity" }
        require(capacity <= MAX_CAPACITY) {
            "capacity must be <= $MAX_CAPACITY but it was $capacity"
        }
    }

    @Volatile
    private var top: Long = 0L
    private val maxIndex = Integer.highestOneBit(capacity * MULTIPLIER - 1) * 2
    private val shift = Integer.numberOfLeadingZeros(maxIndex) + 1
    private val instances = AtomicReferenceArray<T?>(maxIndex + 1)
    private val next = IntArray(maxIndex + 1)

    /**
     * Create a new instance
     */
    protected abstract suspend fun produceInstance(): T

    /**
     * Release all instance resources
     */
    protected open fun disposeInstance(instance: T) {}

    /**
     * Clear state before re-use
     */
    protected open suspend fun clearInstance(instance: T): T = instance

    /**
     * Validate that all is good with this instance
     */
    protected open suspend fun validateInstance(instance: T) {}

    final override suspend fun borrow(): T = tryPop()?.let { clearInstance(it) } ?: produceInstance()

    final override suspend fun recycle(instance: T) {
        validateInstance(instance)
        if (!tryPush(instance)) disposeInstance(instance)
    }

    final override fun dispose() {
        while (true) {
            val instance = tryPop() ?: return
            disposeInstance(instance)
        }
    }

    private fun tryPush(instance: T): Boolean {
        var index = ((System.identityHashCode(instance) * GOLDEN_FRACTION) ushr shift) + 1
        repeat(ATTEMPTS) {
            if (instances.compareAndSet(index, null, instance)) {
                pushTop(index)
                return true
            }
            if (--index == 0) index = maxIndex
        }
        return false
    }

    private fun tryPop(): T? {
        val index = popTop()
        return if (index == 0) null else instances.getAndSet(index, null)
    }

    private fun pushTop(index: Int) {
        require(index > 0) { "index should be > 0" }
        while (true) {
            val top = this.top
            val topVersion = (top shr 32 and 0xffffffffL) + 1L
            val topIndex = (top and 0xffffffffL).toInt()
            val newTop = topVersion shl 32 or index.toLong()
            next[index] = topIndex
            if (Top.compareAndSet(this, top, newTop)) return
        }
    }

    private fun popTop(): Int {
        while (true) {
            val top = this.top
            if (top == 0L) return 0
            val newVersion = (top shr 32 and 0xffffffffL) + 1L
            val topIndex = (top and 0xffffffffL).toInt()
            if (topIndex == 0) return 0
            val next = next[topIndex]
            val newTop = newVersion shl 32 or next.toLong()
            if (Top.compareAndSet(this, top, newTop)) return topIndex
        }
    }

    companion object {
        private val Top = AtomicLongFieldUpdater.newUpdater(DefaultPool::class.java, DefaultPool<*>::top.name)
    }
}
