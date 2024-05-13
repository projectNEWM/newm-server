package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.copy
import io.newm.chain.util.assetFingerprintOf
import io.newm.chain.util.hexToByteArray
import java.math.BigInteger
import java.util.SortedMap

/**
 * Computes the NativeAsset fingerprint
 * Based on: https://cips.cardano.org/cip/CIP-14
 */
fun NativeAsset.fingerprint(): String = assetFingerprintOf(policy, name)

/**
 * Convert a list of NativeAssets into a map where like-assets are combined and the ordering is canonical cbor.
 */
fun List<NativeAsset>.toNativeAssetMap(): MutableMap<String, List<NativeAsset>> {
    val nativeAssetMap = toSortedNativeAssetMap()

    return nativeAssetMap.entries.associate { (key, value) ->
        key to value.sortedWithCanonicalCbor()
    }.toMutableMap()
}

/**
 * Convert to a map of NativeAssets that can be included directly in an OutputUtxo with canonical cbor ordering.
 */
fun List<NativeAsset>.toNativeAssetCborMap(): CborMap? {
    if (isEmpty()) {
        return null
    }
    val nativeAssetMap = toSortedNativeAssetMap()

    return CborMap.create(
        nativeAssetMap.entries.associate { (key, value) ->
            CborByteString.create(key.hexToByteArray()) to
                CborMap.create(
                    value.sortedWithCanonicalCbor().associate { nativeAsset ->
                        CborByteString.create(
                            nativeAsset.name.hexToByteArray()
                        ) to CborInteger.create(nativeAsset.amount.toBigInteger())
                    }
                )
        }
    )
}

/**
 * Convert into a List where amounts of the same asset type are summed.
 */
fun List<NativeAsset>.mergeAmounts(): List<NativeAsset> {
    val map = mutableMapOf<String, Pair<NativeAsset, BigInteger>>()
    for (asset in this) {
        val key = asset.policy + asset.name
        var amount = asset.amount.toBigInteger()
        map[key]?.second?.let { amount += it }
        map[key] = asset to amount
    }
    return map.values.map {
        val amount = it.second.toString()
        if (amount == it.first.amount) {
            it.first
        } else {
            it.first.copy {
                this.amount = amount
            }
        }
    }
}

/**
 * Convert into a sorted map where amounts of the same asset type are summed.
 */
private fun List<NativeAsset>.toSortedNativeAssetMap(): SortedMap<String, List<NativeAsset>> {
    val nativeAssetMap = sortedMapOf<String, List<NativeAsset>>()
    this.forEach { nativeAsset ->
        val updatedNativeAssets: List<NativeAsset> =
            nativeAssetMap[nativeAsset.policy]?.let { nativeAssets ->
                nativeAssets.find { it.name == nativeAsset.name }?.let { na ->
                    val mutableList = nativeAssets.toMutableList()
                    mutableList.remove(na)
                    mutableList.add(
                        na.toBuilder()
                            .setAmount((na.amount.toBigInteger() + nativeAsset.amount.toBigInteger()).toString())
                            .build()
                    )
                    mutableList
                } ?: (nativeAssets + nativeAsset)
            } ?: listOf(nativeAsset)
        nativeAssetMap[nativeAsset.policy] = updatedNativeAssets
    }
    return nativeAssetMap
}

/**
 * Canonical Cbor sort. Shorter name lengths first, then lexicographically
 */
private fun List<NativeAsset>.sortedWithCanonicalCbor(): List<NativeAsset> =
    this.sortedWith { nativeAsset, other ->
        var comparison = nativeAsset.name.length.compareTo(other.name.length)
        if (comparison == 0) {
            comparison = nativeAsset.name.compareTo(other.name)
        }
        comparison
    }
