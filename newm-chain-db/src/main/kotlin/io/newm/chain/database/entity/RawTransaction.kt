package io.newm.chain.database.entity

import kotlinx.serialization.Serializable

@Serializable
data class RawTransaction(
    val id: Long? = null,
    // the block number
    val blockNumber: Long,
    // the slot number
    val slotNumber: Long,
    // the block size
    val blockSize: Int,
    // the block body hash in hex
    val blockBodyHash: String,
    // the major number
    val protocolVersionMajor: Int,
    // the minor number
    val protocolVersionMinor: Int,
    // transaction id
    val txId: String,
    // the raw transaction cbor bytes
    val tx: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawTransaction) return false

        if (id != other.id) return false
        if (blockNumber != other.blockNumber) return false
        if (txId != other.txId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + blockNumber.hashCode()
        result = 31 * result + txId.hashCode()
        return result
    }
}
