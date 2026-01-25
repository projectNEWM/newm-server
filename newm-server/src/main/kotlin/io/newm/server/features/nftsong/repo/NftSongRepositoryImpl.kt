package io.newm.server.features.nftsong.repo

import io.newm.server.features.cardano.model.CardanoNftSong
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.server.features.ethereum.repo.EthereumRepository
import io.newm.server.features.nftsong.model.NftChainMetadata
import io.newm.server.features.nftsong.model.NftChainType
import io.newm.server.features.nftsong.model.NftSong
import io.newm.server.typealiases.UserId

internal class NftSongRepositoryImpl(
    private val cardanoRepository: CardanoRepository,
    private val ethereumRepository: EthereumRepository
) : NftSongRepository {
    override suspend fun getNftSongs(userId: UserId): List<NftSong> {
        val nftSongs = mutableListOf<NftSong>()

        nftSongs += cardanoRepository
            .getWalletNftSongs(userId, includeLegacy = true, useDripDropz = false)
            .map { it.toNftSong() }

        nftSongs += ethereumRepository
            .getWalletNftSongs(userId)
            .map { it.toNftSong() }

        return nftSongs
    }

    private fun CardanoNftSong.toNftSong(): NftSong =
        NftSong(
            id = id,
            title = title,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            duration = duration,
            artists = artists,
            genres = genres,
            moods = moods,
            amount = amount,
            chainType = NftChainType.Cardano,
            chainMetadata = NftChainMetadata.Cardano(
                fingerprint = fingerprint,
                policyId = policyId,
                assetName = assetName,
                isStreamToken = isStreamToken
            )
        )

    private fun EthereumNftSong.toNftSong(): NftSong =
        NftSong(
            id = id,
            title = title,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            duration = duration,
            artists = artists,
            genres = genres,
            moods = moods,
            amount = amount,
            chainType = NftChainType.Ethereum,
            chainMetadata = NftChainMetadata.Ethereum(
                contractAddress = contractAddress,
                tokenType = tokenType,
                tokenId = tokenId
            )
        )
}
