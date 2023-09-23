package io.newm.server.features.cardano.model

import io.newm.server.features.song.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletSong(
    @SerialName("song")
    val song: Song,
    @SerialName("token_amount")
    val tokenAmount: Long,
)
