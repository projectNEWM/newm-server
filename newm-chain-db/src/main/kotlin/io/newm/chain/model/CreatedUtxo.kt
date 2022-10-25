package io.newm.chain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class CreatedUtxo(
    val address: String,
    val addressType: String,
    val stakeAddress: String?,
    val hash: String,
    val ix: Long,
    @Contextual val lovelace: BigInteger,
    val nativeAssets: List<NativeAsset>,
    val cbor: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreatedUtxo) return false

        if (address != other.address) return false
        if (addressType != other.addressType) return false
        if (stakeAddress != other.stakeAddress) return false
        if (hash != other.hash) return false
        if (ix != other.ix) return false
        if (lovelace != other.lovelace) return false
        if (nativeAssets != other.nativeAssets) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + addressType.hashCode()
        result = 31 * result + (stakeAddress?.hashCode() ?: 0)
        result = 31 * result + hash.hashCode()
        result = 31 * result + ix.hashCode()
        result = 31 * result + lovelace.hashCode()
        result = 31 * result + nativeAssets.hashCode()
        return result
    }
}
