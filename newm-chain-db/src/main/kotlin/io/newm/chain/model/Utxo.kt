package io.newm.chain.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class Utxo(
    val address: String,
    val hash: String,
    val ix: Long,
    @Contextual val lovelace: BigInteger,
    val nativeAssets: List<NativeAsset>,
    val datumHash: String?,
    val datum: String?,
    val isInlineDatum: Boolean?,
    val scriptRef: String?,
    val scriptRefVersion: Int?,
)

// MsgResult for utxo query
// [
//  4,
//  [
//    {
//      [h'342FF38B2CB848C1DD54B899CC90EA2B27BDF10D3F8E15C9DF6CBB0A6D0A7255', 0]: [
//        h'000A13BCCE9CA7EBFA534E8E868FEF68445015FA76338A3FF28A087F0ED779399765FEEECC95544A6D447F8EC68A5329EF382634221F49DB07',
//        [
//          1824599, {
//            h'B45FDE8AB44E77AAA825E838715C5F2A1B60013C19EFA639FA96DA96': {h'426C7565436865657365': 333}
//          }
//        ]
//      ],
//      [h'595864B693361B46EBE47A603F651981D3CF6B4361CD98E6198E39CE83993725', 1]: [
//        h'000A13BCCE9CA7EBFA534E8E868FEF68445015FA76338A3FF28A087F0ED779399765FEEECC95544A6D447F8EC68A5329EF382634221F49DB07',
//        [
//          1814813, {
//            h'34250EDD1E9836F5378702FBF9416B709BC140E04F668CC355208518': {h'414441': 1000, h'D090D0BDD182D0B8D09AD0BED0B9D0BD': 1000}, h'B45FDE8AB44E77AAA825E838715C5F2A1B60013C19EFA639FA96DA96': {h'426C7565436865657365': 8667}
//          }
//        ]
//      ],
//      [h'595864B693361B46EBE47A603F651981D3CF6B4361CD98E6198E39CE83993725', 2]: [
//        h'000A13BCCE9CA7EBFA534E8E868FEF68445015FA76338A3FF28A087F0ED779399765FEEECC95544A6D447F8EC68A5329EF382634221F49DB07', 22658532
//      ]
//    }
//  ]
// ]

/**
 * returns a map of policy id to list of NativeAsset values
 */
fun List<Utxo>.toNativeAssetMap(): Map<String, List<NativeAsset>> {
    val nativeAssetMap = sortedMapOf<String, List<NativeAsset>>()
    this.forEach { utxo ->
        utxo.nativeAssets.forEach { nativeAsset ->
            val updatedNativeAssets: List<NativeAsset> =
                nativeAssetMap[nativeAsset.policy]?.let { nativeAssets ->
                    nativeAssets.find { it.name == nativeAsset.name }?.let {
                        val prevAmount = it.amount
                        val mutableList = nativeAssets.toMutableList()
                        mutableList.remove(it)
                        mutableList.add(NativeAsset(nativeAsset.name, nativeAsset.policy, nativeAsset.amount + prevAmount))
                        mutableList
                    } ?: (nativeAssets + nativeAsset)
                } ?: listOf(nativeAsset)
            nativeAssetMap[nativeAsset.policy] = updatedNativeAssets
        }
    }

    return nativeAssetMap
}

@JvmName("toNativeAssetMapNativeAsset")
fun List<NativeAsset>.toNativeAssetMap(): Map<String, List<NativeAsset>> {
    val nativeAssetMap = sortedMapOf<String, List<NativeAsset>>()
    this.forEach { nativeAsset ->
        val updatedNativeAssets: List<NativeAsset> =
            nativeAssetMap[nativeAsset.policy]?.let { nativeAssets ->
                nativeAssets.find { it.name == nativeAsset.name }?.let {
                    val prevAmount = it.amount
                    val mutableList = nativeAssets.toMutableList()
                    mutableList.remove(it)
                    mutableList.add(NativeAsset(nativeAsset.name, nativeAsset.policy, nativeAsset.amount + prevAmount))
                    mutableList
                } ?: (nativeAssets + nativeAsset)
            } ?: listOf(nativeAsset)
        nativeAssetMap[nativeAsset.policy] = updatedNativeAssets
    }

    return nativeAssetMap
}
