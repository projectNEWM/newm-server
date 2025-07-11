package io.newm.server.features.ethereum.repo

import io.newm.server.features.ethereum.model.EthereumNftSong

interface EthereumRepository {
    suspend fun getNftSongs(ownerAddress: String): List<EthereumNftSong>
}
