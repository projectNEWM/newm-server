package io.newm.chain.model

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class NativeAsset(
    val policy: String,
    val name: String,
    @Contextual val amount: BigInteger,
) : Comparable<NativeAsset> {
    /**
     * Sort canonical cbor
     */
    override fun compareTo(other: NativeAsset): Int {
        var comparison = this.policy.compareTo(other.policy)
        if (comparison == 0) {
            comparison = this.name.length.compareTo(other.name.length)
            if (comparison == 0) {
                comparison = this.name.compareTo(other.name)
            }
        }
        return comparison
    }
}
