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
    val datumHash: String?,
    val datum: String?,
    val scriptRef: String?,
    val nativeAssets: List<NativeAsset>,
    val cbor: ByteArray?,
    val paymentCred: String?,
    val stakeCred: String?,
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
        if (datumHash != other.datumHash) return false
        if (datum != other.datum) return false
        if (scriptRef != other.scriptRef) return false
        if (nativeAssets != other.nativeAssets) return false
        if (cbor != null) {
            if (other.cbor == null) return false
            if (!cbor.contentEquals(other.cbor)) return false
        } else if (other.cbor != null) return false
        if (paymentCred != other.paymentCred) return false
        return stakeCred == other.stakeCred
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + addressType.hashCode()
        result = 31 * result + (stakeAddress?.hashCode() ?: 0)
        result = 31 * result + hash.hashCode()
        result = 31 * result + ix.hashCode()
        result = 31 * result + lovelace.hashCode()
        result = 31 * result + (datumHash?.hashCode() ?: 0)
        result = 31 * result + (datum?.hashCode() ?: 0)
        result = 31 * result + (scriptRef?.hashCode() ?: 0)
        result = 31 * result + nativeAssets.hashCode()
        result = 31 * result + (cbor?.contentHashCode() ?: 0)
        result = 31 * result + (paymentCred?.hashCode() ?: 0)
        result = 31 * result + (stakeCred?.hashCode() ?: 0)
        return result
    }
}
