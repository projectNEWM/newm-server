package io.newm.server.features.nftsong.repo

import io.newm.server.features.nftsong.model.NftSong
import io.newm.server.typealiases.UserId

interface NftSongRepository {
    suspend fun getNftSongs(userId: UserId): List<NftSong>
}
