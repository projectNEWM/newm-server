package io.newm.server.features.ethereum.repo

import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.server.typealiases.UserId

interface EthereumRepository {
    suspend fun getWalletNftSongs(userId: UserId): List<EthereumNftSong>
}
