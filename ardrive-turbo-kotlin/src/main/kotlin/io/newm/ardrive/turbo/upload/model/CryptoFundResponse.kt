package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class CryptoFundResponse(
    val id: String,
    val quantity: String,
    val owner: String,
    val winc: String,
    val token: String,
    val status: String,
    val recipient: String? = null,
    val block: Int? = null,
    val target: String,
    val reward: String? = null,
)
