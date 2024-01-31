package io.newm.chain.cardano

import io.newm.kogmios.protocols.model.Asset

/**
 * Canonical Cbor sort. Shorter name lengths first, then lexicographically
 */
fun List<Asset>.sortedWithCanonicalCbor(): List<Asset> =
    this.sortedWith { nativeAsset, other ->
        var comparison = nativeAsset.policyId.compareTo(other.policyId)
        if (comparison == 0) {
            comparison = nativeAsset.name.length.compareTo(other.name.length)
            if (comparison == 0) {
                comparison = nativeAsset.name.compareTo(other.name)
            }
        }
        comparison
    }
