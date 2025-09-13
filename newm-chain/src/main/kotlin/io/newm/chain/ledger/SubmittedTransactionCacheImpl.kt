package io.newm.chain.ledger

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborReader
import io.newm.chain.util.Constants.TX_TTL_INDEX
import io.newm.chain.util.getInstantAtSlot
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    override fun get(txId: String): ByteArray? {
        val txBytes = submittedTransactionCache.getIfPresent(txId) ?: return null
        return if (isExpired(txBytes)) {
            // Remove expired transaction eagerly
            synchronized(orderedSubmittedTransactionMap) { orderedSubmittedTransactionMap.remove(txId) }
            submittedTransactionCache.invalidate(txId)
            null
        } else {
            txBytes
        }
    }

    override fun forEach(action: (Map.Entry<String, ByteArray>) -> Unit) {
        val toRemove = mutableListOf<String>()
        synchronized(orderedSubmittedTransactionMap) {
            orderedSubmittedTransactionMap.forEach { entry ->
                if (isExpired(entry.value)) {
                    toRemove.add(entry.key)
                } else {
                    action(entry)
                }
            }
            if (toRemove.isNotEmpty()) {
                toRemove.forEach { key ->
                    orderedSubmittedTransactionMap.remove(key)
                    submittedTransactionCache.invalidate(key)
                }
            }
        }
    }

    override val keys: Set<String>
        get() =
            synchronized(orderedSubmittedTransactionMap) {
                // Filter out expired keys lazily
                val toRemove = mutableListOf<String>()
                val validKeys = orderedSubmittedTransactionMap.entries
                    .filter { (k, v) ->
                        if (isExpired(v)) {
                            toRemove.add(k)
                            false
                        } else {
                            true
                        }
                    }.map { it.key }
                    .toSet()
                if (toRemove.isNotEmpty()) {
                    toRemove.forEach { key ->
                        orderedSubmittedTransactionMap.remove(key)
                        submittedTransactionCache.invalidate(key)
                    }
                }
                validKeys
            }

    private fun isExpired(txSigned: ByteArray): Boolean {
        val validityEndSlot = extractValidityEndSlot(txSigned) ?: return false
        val endInstant = getInstantAtSlot(validityEndSlot)
        // Expired if current time is after the validity end instant
        return Instant.now().isAfter(endInstant)
    }

    private fun extractValidityEndSlot(txSigned: ByteArray): Long? =
        try {
            // Extract just the TTL field from the signed transaction CBOR. No need to use the full deserialization.
            val top = CborReader.createFromByteArray(txSigned).readDataItem() as? CborArray ?: return null
            val txBody = top.elementAt(0) as? CborMap ?: return null
            val ttl = txBody.get(TX_TTL_INDEX) as? CborInteger
            ttl?.longValue()
        } catch (_: Throwable) {
            null
        }

    suspend inline fun <T> withLock(action: () -> T): T = cacheMutex.withLock(null, action)
}
