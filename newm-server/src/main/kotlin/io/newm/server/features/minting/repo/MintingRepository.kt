package io.newm.server.features.minting.repo

import io.newm.server.features.song.model.Song

interface MintingRepository {
    suspend fun mint(song: Song)
}
