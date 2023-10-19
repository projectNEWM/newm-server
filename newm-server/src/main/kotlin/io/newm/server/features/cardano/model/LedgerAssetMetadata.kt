package io.newm.server.features.cardano.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LedgerAssetMetadata(
    @SerialName("keyType")
    val keyType: String,
    @SerialName("key")
    val key: String,
    @SerialName("valueType")
    val valueType: String,
    @SerialName("value")
    val value: String,
    @SerialName("nestLevel")
    val nestLevel: Int,
    @SerialName("children")
    val children: List<LedgerAssetMetadata>,
)
