package io.newm.server.features.nftsong.repo

import io.newm.server.features.cardano.model.CardanoNftSong
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.server.features.ethereum.repo.EthereumRepository
import io.newm.server.features.nftsong.model.NftChainMetadata
import io.newm.server.features.nftsong.model.NftSong
import io.newm.server.typealiases.UserId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class NftSongRepositoryImpl(
    private val cardanoRepository: CardanoRepository,
    private val ethereumRepository: EthereumRepository
) : NftSongRepository {
    override suspend fun getNftSongs(userId: UserId): List<NftSong> =
        coroutineScope {
            val cardanoJob = async {
                cardanoRepository
                    .getWalletNftSongs(userId, includeLegacy = true, useDripDropz = false)
                    .map { it.toNftSong() }
            }
            val ethereumJob = async {
                ethereumRepository
                    .getWalletNftSongs(userId)
                    .map { it.toNftSong() }
            }
            cardanoJob.await() + ethereumJob.await()
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
            chainMetadata = NftChainMetadata.Ethereum(
                contractAddress = contractAddress,
                tokenType = tokenType,
                tokenId = tokenId
            )
        )
}
