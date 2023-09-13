package io.newm.server.features.song.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class MintPaymentResponse(
    @SerialName("cborHex")
    val cborHex: String,
    @Contextual
    @SerialName("usdPrice")
    val usdPrice: BigInteger? = null,
)
