package io.newm.server.features.cardano.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetWalletSongsResponse(
    @SerialName("songs")
    val songs: List<WalletSong>,
    @SerialName("total")
    val total: Long,
    @SerialName("offset")
    val offset: Int,
    @SerialName("limit")
    val limit: Int,
)
