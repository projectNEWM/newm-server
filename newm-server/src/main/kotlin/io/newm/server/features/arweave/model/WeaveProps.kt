package io.newm.server.features.arweave.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaveProps(
    @SerialName("arweaveWalletJson")
    val arweaveWalletJson: String,
    @SerialName("files")
    val files: List<WeaveFile>,
)
