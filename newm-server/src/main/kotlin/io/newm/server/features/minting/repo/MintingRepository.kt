package io.newm.server.features.minting.repo

import io.newm.server.features.song.model.Song

interface MintingRepository {

    /**
     * Mints the song onto the blockchain and returns the successful transactionId
     */
    suspend fun mint(song: Song): String
}
