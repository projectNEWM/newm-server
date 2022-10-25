package io.newm.chain.model

import kotlinx.serialization.Serializable

@Serializable
data class NativeAssetMetadata(
    val id: Long? = null,
    val assetName: String,
    val assetPolicy: String,
    val metadataName: String,
    val metadataImage: String,
    val metadataDescription: String?
)
