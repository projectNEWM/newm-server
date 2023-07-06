package io.newm.server.features.arweave.repo

import io.newm.server.features.song.model.Song
import java.math.BigDecimal

interface ArweaveRepository {
    suspend fun getWalletAddress(): String

    suspend fun getWalletARBalance(): BigDecimal

    suspend fun uploadSongAssets(song: Song)
}
