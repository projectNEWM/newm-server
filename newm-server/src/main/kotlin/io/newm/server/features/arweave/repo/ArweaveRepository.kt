package io.newm.server.features.arweave.repo

import io.newm.server.features.song.model.Song

interface ArweaveRepository {
    suspend fun getWalletAddress(): String
    suspend fun uploadSongAssets(song: Song)
}
