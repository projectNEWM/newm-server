package io.newm.chain.ledger

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration

/**
 * Temporary storage for submitted transactions so we can look them up when they land on a block
 */
class SubmittedTransactionCacheImpl : SubmittedTransactionCache {
    val cacheMutex = Mutex()

    /**
     * Ordered list of submitted transactions
     */
    private val orderedSubmittedTransactionMap = LinkedHashMap<String, ByteArray>()

    /**
     * Hold transactions in case we need to re-submit due to rollbacks
     */
    private val submittedTransactionCache: Cache<String, ByteArray> =
        Caffeine
            .newBuilder()
            .removalListener(
                RemovalListener<String, ByteArray> { key, _, cause ->
                    if (cause == RemovalCause.REPLACED) {
                        return@RemovalListener
                    }
                    synchronized(orderedSubmittedTransactionMap) {
                        orderedSubmittedTransactionMap.remove(key)
                    }
                }
            ).expireAfterWrite(Duration.ofHours(48))
            .build()

    override fun put(
        txId: String,
        txSigned: ByteArray
    ) {
        synchronized(orderedSubmittedTransactionMap) {
            orderedSubmittedTransactionMap[txId] = txSigned
            submittedTransactionCache.put(txId, txSigned)
        }
    }

    override fun get(txId: String): ByteArray? = submittedTransactionCache.getIfPresent(txId)

    override fun forEach(action: (Map.Entry<String, ByteArray>) -> Unit) {
        synchronized(orderedSubmittedTransactionMap) {
            orderedSubmittedTransactionMap.forEach(action)
        }
    }

    override val keys: Set<String>
        get() =
            synchronized(orderedSubmittedTransactionMap) {
                orderedSubmittedTransactionMap.keys
            }

    suspend inline fun <T> withLock(action: () -> T): T = cacheMutex.withLock(null, action)
}
