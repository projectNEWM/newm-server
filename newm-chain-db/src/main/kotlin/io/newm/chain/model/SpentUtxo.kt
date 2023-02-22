package io.newm.chain.model

import kotlinx.serialization.Serializable

@Serializable
data class SpentUtxo(
    val transactionSpent: String,
    val hash: String,
    val ix: Long,
) {
    /**
     * We intentionally ignore transactionSpent for equals()
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpentUtxo

        if (hash != other.hash) return false
        if (ix != other.ix) return false

        return true
    }

    /**
     * We intentionally ignore transactionSpent for hashCode()
     */
    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + ix.hashCode()
        return result
    }
}
