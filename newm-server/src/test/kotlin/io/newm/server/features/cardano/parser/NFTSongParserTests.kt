package io.newm.server.features.cardano.parser

import com.google.common.truth.Truth.assertThat
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.chain.grpc.copy
import io.newm.chain.grpc.queryByNativeAssetRequest
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.model.NFTSong
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

private const val TEST_HOST = "newmchain.newm.studio"
private const val TEST_PORT = 3737
private const val TEST_SECURE = true

// DO NOT COMMIT THIS TOKEN
private const val JWT_TOKEN = "<JWT_TOKEN_HERE_DO_NOT_COMMIT>"

@Disabled("Disabled - require JWT Token")
class NFTSongParserTests : BaseApplicationTests() {

    @Test
    fun `NEWM_0 - MURS Bigger Dreams, CIP-60 V1, Single`() = runBlocking {
        val expectedSong = NFTSong(
            id = "ar://P141o0RDAjSYlVQgTDgHNAORQTkMYIVCprmD_dKMVss".toId(),
            policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
            assetName = "4e45574d5f30",
            amount = 1,
            title = "Bigger Dreams",
            imageUrl = "https://arweave.net/CuPFY2Ln7yUUhJX09G530kdPf93eGhAVlhjrtR7Jh5w",
            audioUrl = "https://arweave.net/P141o0RDAjSYlVQgTDgHNAORQTkMYIVCprmD_dKMVss",
            duration = 240,
            artists = listOf("MURS"),
            genres = listOf("Hip Hop", "Rap"),
            moods = listOf("Feel Good")
        )
        val actualSongs = buildClient().queryNFTSongs(expectedSong.policyId, expectedSong.assetName)
        assertThat(actualSongs.size).isEqualTo(1)
        assertThat(actualSongs.first()).isEqualTo(expectedSong)
    }

    @Test
    fun `NEWM_5 - Daisuke, CIP-60 V1, Single`() = runBlocking {
        val expectedSong = NFTSong(
            id = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0".toId(),
            policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
            assetName = "4e45574d5f35",
            amount = 1,
            title = "Daisuke",
            imageUrl = "https://arweave.net/GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
            audioUrl = "https://arweave.net/QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
            duration = 200,
            artists = listOf("Danketsu", "Mirai Music", "NSTASIA"),
            genres = listOf("Pop", "House", "Tribal"),
            moods = listOf("Spiritual")
        )
        val actualSongs = buildClient().queryNFTSongs(expectedSong.policyId, expectedSong.assetName)
        assertThat(actualSongs.size).isEqualTo(1)
        assertThat(actualSongs.first()).isEqualTo(expectedSong)
    }

    @Test
    fun `OddShapeShadow, CIP-60 V1, Single`() = runBlocking {
        val expectedSong = NFTSong(
            id = "ipfs://QmaiQ2mHc2LhkApA5cXPk8WfV6923ndgVDQoAtdHsSkXWE".toId(),
            policyId = "7ad9d1ddb00adee7939f8027e5258a561878fff8761993afb311e56f",
            assetName = "4f5353445245414d4c4f4649",
            amount = 1,
            title = "Smoke and Fire - Dream Lofi",
            imageUrl = "https://ipfs.io/ipfs/QmUa8NEsbSRTsdsKSqkHb8ZgEWcoBppwA3RfecDhFGkG6f",
            audioUrl = "https://ipfs.io/ipfs/QmaiQ2mHc2LhkApA5cXPk8WfV6923ndgVDQoAtdHsSkXWE",
            duration = 154,
            artists = listOf("OddShapeShadow"),
            genres = listOf("lofi", "electronic"),
            moods = emptyList()
        )
        val actualSongs = buildClient().queryNFTSongs(expectedSong.policyId, expectedSong.assetName)
        assertThat(actualSongs.size).isEqualTo(1)
        assertThat(actualSongs.first()).isEqualTo(expectedSong)
    }

    @Test
    fun `SickCity442, CIP-60 V2, Single`() = runBlocking {
        val expectedSong = NFTSong(
            id = "ipfs://QmNPg1BTnyouUL1uiHyWc4tQZXH5anEz4jmua7iidwEbiE".toId(),
            policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
            assetName = "5369636b43697479343432",
            amount = 1,
            title = "Paper Route",
            imageUrl = "https://ipfs.io/ipfs/QmNNuBTgPwqoWyNMtSwtQtb8ycVF1TrkrsUCGaFWqXvjkr",
            audioUrl = "https://ipfs.io/ipfs/QmNPg1BTnyouUL1uiHyWc4tQZXH5anEz4jmua7iidwEbiE",
            duration = 225,
            artists = listOf("Mikey Mo the MC"),
            genres = listOf("rap", "hip hop"),
            moods = emptyList()
        )
        val actualSongs = buildClient().queryNFTSongs(expectedSong.policyId, expectedSong.assetName)
        assertThat(actualSongs.size).isEqualTo(1)
        assertThat(actualSongs.first()).isEqualTo(expectedSong)
    }

    @Test
    fun `Jamison Daniel-Studio Life, Legacy, Multiple`() = runBlocking {
        val policyId = "fb818dd32539209755211ab01cde517b044a742f1bc52e5cc57b25d9"
        val assetName = "4a616d69736f6e44616e69656c53747564696f4c696665323138"
        val expectedSongs = listOf(
            NFTSong(
                id = "ipfs://QmduC7pkR14K3mhmvEazoyzGsMWVF4ji45HZ1XfEracKLv".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Finally (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmduC7pkR14K3mhmvEazoyzGsMWVF4ji45HZ1XfEracKLv",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmW9sHugSArzf29JPuEC2MqjtbsNkDjd9xNUxZFLDXSDUY".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Funky Squirrel (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmW9sHugSArzf29JPuEC2MqjtbsNkDjd9xNUxZFLDXSDUY",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://Qmb8fm7CkzscjjoJGVp3p7qjSVMknsk27d3cwjqM26ELVB".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Weekend Ride (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/Qmb8fm7CkzscjjoJGVp3p7qjSVMknsk27d3cwjqM26ELVB",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmTwvwpgE9Fx6QZsjbXe5STHb3WVmaDuxFzafqCPueCmqc".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Rave Culture (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmTwvwpgE9Fx6QZsjbXe5STHb3WVmaDuxFzafqCPueCmqc",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmTETraR8WvExCaanc5aGT8EAUgCojyN8YSZYbGgmzVfja".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Vibrate (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmTETraR8WvExCaanc5aGT8EAUgCojyN8YSZYbGgmzVfja",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://Qmdfr4PvuiZhi3a6EaDupGN6R33PKSy5kntwgFEzLQnPLR".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Top 40's (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/Qmdfr4PvuiZhi3a6EaDupGN6R33PKSy5kntwgFEzLQnPLR",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmSp4Cn7qrhLTovezS1ii7ct1VAPK6Gotd2GnxnBc6ngSv".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Acid Trip (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmSp4Cn7qrhLTovezS1ii7ct1VAPK6Gotd2GnxnBc6ngSv",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmV8ihv8R6cCKsFJyFP8fhnnQjeKjS7HAAjmxMgUPftmw6".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "For The Win (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmV8ihv8R6cCKsFJyFP8fhnnQjeKjS7HAAjmxMgUPftmw6",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
            NFTSong(
                id = "ipfs://QmWux5UpX6BtYQ7pjugqRh6ySa2vVJN12iSC2AB1cAQynU".toId(),
                policyId = policyId,
                assetName = assetName,
                amount = 1,
                title = "Sunday Sermon (Master 2021)",
                imageUrl = "https://ipfs.io/ipfs/QmSVK4ts4E4UK9A7QeHPPCXsfv8aPaoH7dpJiatZQHUtpV",
                audioUrl = "https://ipfs.io/ipfs/QmWux5UpX6BtYQ7pjugqRh6ySa2vVJN12iSC2AB1cAQynU",
                duration = -1L,
                artists = emptyList(),
                genres = emptyList(),
                moods = emptyList()
            ),
        )

        val actualSongs = buildClient().queryNFTSongs(policyId, assetName)
        assertThat(actualSongs.size).isEqualTo(9)
        assertThat(actualSongs).isEqualTo(expectedSongs)
    }

    private fun String.toId(): UUID = UUID.nameUUIDFromBytes(toByteArray())

    private suspend fun NewmChainCoroutineStub.queryNFTSongs(
        policyId: String,
        assetName: String
    ): List<NFTSong> {
        val asset = NativeAsset.getDefaultInstance().copy {
            policy = policyId
            name = assetName
            amount = "1"
        }
        return queryLedgerAssetMetadataListByNativeAsset(
            queryByNativeAssetRequest {
                policy = policyId
                name = assetName
            }
        ).ledgerAssetMetadataList.toNFTSongs(asset)
    }

    private fun buildClient(): NewmChainCoroutineStub {
        val channel = ManagedChannelBuilder.forAddress(TEST_HOST, TEST_PORT).apply {
            if (TEST_SECURE) {
                useTransportSecurity()
            } else {
                usePlaintext()
            }
        }.build()
        return NewmChainCoroutineStub(channel).withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().apply {
                    put(
                        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Bearer $JWT_TOKEN"
                    )
                }
            )
        )
    }
}
