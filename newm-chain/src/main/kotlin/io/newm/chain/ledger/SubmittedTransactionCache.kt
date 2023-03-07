package io.newm.chain.ledger

interface SubmittedTransactionCache {
    val keys: Set<String>

    fun put(txId: String, txSigned: ByteArray)
    fun get(txId: String): ByteArray?
    fun forEach(action: (Map.Entry<String, ByteArray>) -> Unit)
}
