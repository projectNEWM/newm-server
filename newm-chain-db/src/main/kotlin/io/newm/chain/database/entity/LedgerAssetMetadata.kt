package io.newm.chain.database.entity

import kotlinx.serialization.Serializable

@Serializable
data class LedgerAssetMetadata(
    val id: Long? = null,
    val assetId: Long,
    val keyType: String,
    val key: String,
    val valueType: String,
    val value: String,
    val nestLevel: Int = 0,
    val children: List<LedgerAssetMetadata> = emptyList(),
)
