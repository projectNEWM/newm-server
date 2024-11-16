package io.newm.server.features.cardano.parser

import com.google.common.truth.Truth.assertThat
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.chain.grpc.copy
import io.newm.chain.grpc.queryByNativeAssetRequest
import io.newm.chain.util.assetNameToHexString
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.model.NFTSong
import io.newm.server.features.nftcdn.repo.NftCdnRepository
import io.newm.shared.koin.inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
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
    private lateinit var newmChainClient: NewmChainCoroutineStub

    @BeforeAll
    fun init() {
        newmChainClient = buildClient()
    }

    @Test
    fun `NEWM_0 - MURS Bigger Dreams, CIP-60 V1, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ar://P141o0RDAjSYlVQgTDgHNAORQTkMYIVCprmD_dKMVss".toId(),
                fingerprint = "asset19dx98tjqckn26yk5hcse4zm6m0aj4gf7z0z378",
                policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
                assetName = "NEWM_0",
                isStreamToken = true,
                amount = 1,
                title = "Bigger Dreams",
                imageUrl = "https://arweave.net/CuPFY2Ln7yUUhJX09G530kdPf93eGhAVlhjrtR7Jh5w",
                audioUrl = "https://arweave.net/P141o0RDAjSYlVQgTDgHNAORQTkMYIVCprmD_dKMVss",
                duration = 240,
                artists = listOf("MURS"),
                genres = listOf("Hip Hop", "Rap"),
                moods = listOf("Feel Good")
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, true, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `NEWM_0 - MURS Bigger Dreams, CIP-60 V1, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset19dx98tjqckn26yk5hcse4zm6m0aj4gf7z0z378"
            val expectedSong = NFTSong(
                id = "ar://P141o0RDAjSYlVQgTDgHNAORQTkMYIVCprmD_dKMVss".toId(),
                fingerprint = fingerprint,
                policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
                assetName = "NEWM_0",
                isStreamToken = true,
                amount = 1,
                title = "Bigger Dreams",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 240,
                artists = listOf("MURS"),
                genres = listOf("Hip Hop", "Rap"),
                moods = listOf("Feel Good")
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, true, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `NEWM_5 - Daisuke, CIP-60 V1, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0".toId(),
                fingerprint = "asset1effvlkkw02m9ft3ymlkfld8mhlq05wc2hal5du",
                policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
                assetName = "NEWM_5",
                isStreamToken = true,
                amount = 1,
                title = "Daisuke",
                imageUrl = "https://arweave.net/GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                audioUrl = "https://arweave.net/QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                duration = 200,
                artists = listOf("Danketsu", "Mirai Music", "NSTASIA"),
                genres = listOf("Pop", "House", "Tribal"),
                moods = listOf("Spiritual")
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, true, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `NEWM_5 - Daisuke, CIP-60 V1, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1effvlkkw02m9ft3ymlkfld8mhlq05wc2hal5du"
            val expectedSong = NFTSong(
                id = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0".toId(),
                fingerprint = fingerprint,
                policyId = "46e607b3046a34c95e7c29e47047618dbf5e10de777ba56c590cfd5c",
                assetName = "NEWM_5",
                isStreamToken = true,
                amount = 1,
                title = "Daisuke",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 200,
                artists = listOf("Danketsu", "Mirai Music", "NSTASIA"),
                genres = listOf("Pop", "House", "Tribal"),
                moods = listOf("Spiritual")
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, true, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `OddShapeShadow, CIP-60 V1, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmaiQ2mHc2LhkApA5cXPk8WfV6923ndgVDQoAtdHsSkXWE".toId(),
                fingerprint = "asset13ht8rn89zwvchfd4d34707xvcrr3clzkgdgj6p",
                policyId = "7ad9d1ddb00adee7939f8027e5258a561878fff8761993afb311e56f",
                assetName = "OSSDREAMLOFI",
                isStreamToken = false,
                amount = 1,
                title = "Smoke and Fire - Dream Lofi",
                imageUrl = "https://ipfs.io/ipfs/QmUa8NEsbSRTsdsKSqkHb8ZgEWcoBppwA3RfecDhFGkG6f",
                audioUrl = "https://ipfs.io/ipfs/QmaiQ2mHc2LhkApA5cXPk8WfV6923ndgVDQoAtdHsSkXWE",
                duration = 154,
                artists = listOf("OddShapeShadow"),
                genres = listOf("lofi", "electronic"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `OddShapeShadow, CIP-60 V1, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset13ht8rn89zwvchfd4d34707xvcrr3clzkgdgj6p"
            val expectedSong = NFTSong(
                id = "ipfs://QmaiQ2mHc2LhkApA5cXPk8WfV6923ndgVDQoAtdHsSkXWE".toId(),
                fingerprint = fingerprint,
                policyId = "7ad9d1ddb00adee7939f8027e5258a561878fff8761993afb311e56f",
                assetName = "OSSDREAMLOFI",
                isStreamToken = false,
                amount = 1,
                title = "Smoke and Fire - Dream Lofi",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 154,
                artists = listOf("OddShapeShadow"),
                genres = listOf("lofi", "electronic"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity442, CIP-60 V2, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmNPg1BTnyouUL1uiHyWc4tQZXH5anEz4jmua7iidwEbiE".toId(),
                fingerprint = "asset1w90kz4y6zpgndgk8a837g3f2n4ujtlx0mgpdhw",
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity442",
                isStreamToken = false,
                amount = 1,
                title = "Paper Route",
                imageUrl = "https://ipfs.io/ipfs/QmNNuBTgPwqoWyNMtSwtQtb8ycVF1TrkrsUCGaFWqXvjkr",
                audioUrl = "https://ipfs.io/ipfs/QmNPg1BTnyouUL1uiHyWc4tQZXH5anEz4jmua7iidwEbiE",
                duration = 225,
                artists = listOf("Mikey Mo the MC"),
                genres = listOf("rap", "hip hop"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity442, CIP-60 V2, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1w90kz4y6zpgndgk8a837g3f2n4ujtlx0mgpdhw"
            val expectedSong = NFTSong(
                id = "ipfs://QmNPg1BTnyouUL1uiHyWc4tQZXH5anEz4jmua7iidwEbiE".toId(),
                fingerprint = fingerprint,
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity442",
                isStreamToken = false,
                amount = 1,
                title = "Paper Route",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 225,
                artists = listOf("Mikey Mo the MC"),
                genres = listOf("rap", "hip hop"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `JukeBox501, CIP-60 V3, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmbchFjoQFxMTzecKnXnf7YaAretRJzVyq2qNEepnRinTv".toId(),
                fingerprint = "asset1skssrzvfqr4qfpp7fhv5xu38t95caxumthvq4x",
                policyId = "ecbbf5849b532038a2449c62f295cb89120db4549e9cb5372428adad",
                assetName = "JukeBox501",
                isStreamToken = false,
                amount = 1,
                title = "Waterfalls",
                imageUrl = "https://ipfs.io/ipfs/QmYuTT1ci2Zj7QTEnHDwQ6pf422fHzFxXxaQPyptqvL5ki",
                audioUrl = "https://ipfs.io/ipfs/QmbchFjoQFxMTzecKnXnf7YaAretRJzVyq2qNEepnRinTv",
                duration = 173L,
                artists = listOf("OddShapeShadow"),
                genres = listOf("d&b", "electronic"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `JukeBox501, CIP-60 V3, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1skssrzvfqr4qfpp7fhv5xu38t95caxumthvq4x"
            val expectedSong = NFTSong(
                id = "ipfs://QmbchFjoQFxMTzecKnXnf7YaAretRJzVyq2qNEepnRinTv".toId(),
                fingerprint = fingerprint,
                policyId = "ecbbf5849b532038a2449c62f295cb89120db4549e9cb5372428adad",
                assetName = "JukeBox501",
                isStreamToken = false,
                amount = 1,
                title = "Waterfalls",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 173L,
                artists = listOf("OddShapeShadow"),
                genres = listOf("d&b", "electronic"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity343, Legacy, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmcBtkxaKsFK3wvNHxULhuRhzaabqoX6Ryor4PvnaqcUSb".toId(),
                fingerprint = "asset1twzjexu7m9drdznjrz47f3320jxry7erhnx3e3",
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity343",
                isStreamToken = false,
                amount = 1,
                title = "It Gets Better",
                imageUrl = "https://ipfs.io/ipfs/QmY6mAm1L6G4XSDtUKiNdYPkGXAKXXU4HXzEthxbMhzr8U",
                audioUrl = "https://ipfs.io/ipfs/QmcBtkxaKsFK3wvNHxULhuRhzaabqoX6Ryor4PvnaqcUSb",
                duration = -1L,
                artists = listOf("Memellionaires"),
                genres = listOf("Pop-Rock", "Alternative"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity343, Legacy, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1twzjexu7m9drdznjrz47f3320jxry7erhnx3e3"
            val expectedSong = NFTSong(
                id = "ipfs://QmcBtkxaKsFK3wvNHxULhuRhzaabqoX6Ryor4PvnaqcUSb".toId(),
                fingerprint = fingerprint,
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity343",
                isStreamToken = false,
                amount = 1,
                title = "It Gets Better",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = -1L,
                artists = listOf("Memellionaires"),
                genres = listOf("Pop-Rock", "Alternative"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity344, Legacy, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb".toId(),
                fingerprint = "asset163r3tg4qggphswslxmfezu2gfevs35433pqz3e",
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity344",
                isStreamToken = false,
                amount = 1,
                title = "4EVR",
                imageUrl = "https://ipfs.io/ipfs/QmSXPyRe9KzmY18R64pdzMyvMiacu1C8eosZWepw1Eexme",
                audioUrl = "https://ipfs.io/ipfs/QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb",
                duration = 189L,
                artists = listOf("Irie Reyna"),
                genres = listOf("r&b", "soul"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity344, Legacy, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset163r3tg4qggphswslxmfezu2gfevs35433pqz3e"
            val expectedSong = NFTSong(
                id = "ipfs://QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb".toId(),
                fingerprint = fingerprint,
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity344",
                isStreamToken = false,
                amount = 1,
                title = "4EVR",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 189L,
                artists = listOf("Irie Reyna"),
                genres = listOf("r&b", "soul"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity349, Legacy, Single`() =
        runBlocking {
            val expectedSong = NFTSong(
                id = "ipfs://QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr".toId(),
                fingerprint = "asset1n8jsja05t2dlsy3e7wzjcr2a8724qmgmnwv669",
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity349",
                isStreamToken = false,
                amount = 1,
                title = "You, I, and The Ocean",
                imageUrl = "https://ipfs.io/ipfs/QmdZtxeKLGTXGkpcWkWznCZW7qsyiGA78dFcDwy9cA4D1d",
                audioUrl = "https://ipfs.io/ipfs/QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr",
                duration = 267L,
                artists = listOf("Sam Katman"),
                genres = listOf("singer-songwriter", "folk pop"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity349, Legacy, Single, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1n8jsja05t2dlsy3e7wzjcr2a8724qmgmnwv669"
            val expectedSong = NFTSong(
                id = "ipfs://QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr".toId(),
                fingerprint = fingerprint,
                policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                assetName = "SickCity349",
                isStreamToken = false,
                amount = 1,
                title = "You, I, and The Ocean",
                imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                duration = 267L,
                artists = listOf("Sam Katman"),
                genres = listOf("singer-songwriter", "folk pop"),
                moods = emptyList()
            )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `Jamison Daniel-Studio Life, Legacy, Multiple`() =
        runBlocking {
            val policyId = "fb818dd32539209755211ab01cde517b044a742f1bc52e5cc57b25d9"
            val assetName = "JamisonDanielStudioLife218"
            val fingerprint = "asset1njl2quag7haj4xcwfckn4rqprrvcwlr08z34ua"
            val expectedSongs = listOf(
                NFTSong(
                    id = "ipfs://QmduC7pkR14K3mhmvEazoyzGsMWVF4ji45HZ1XfEracKLv".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
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

            val actualSongs = newmChainClient.queryNFTSongs(policyId, assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(9)
            assertThat(actualSongs).isEqualTo(expectedSongs)
        }

    @Test
    fun `Jamison Daniel-Studio Life, Legacy, Multiple, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val fingerprint = "asset1njl2quag7haj4xcwfckn4rqprrvcwlr08z34ua"
            val policyId = "fb818dd32539209755211ab01cde517b044a742f1bc52e5cc57b25d9"
            val assetName = "JamisonDanielStudioLife218"
            // NOTE: notice that "files/0" is an image file, so 1st audio file is at "files/1"
            // https://cexplorer.io/asset/asset1njl2quag7haj4xcwfckn4rqprrvcwlr08z34ua/metadata#data
            val expectedSongs = listOf(
                NFTSong(
                    id = "ipfs://QmduC7pkR14K3mhmvEazoyzGsMWVF4ji45HZ1XfEracKLv".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Finally (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 1),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmW9sHugSArzf29JPuEC2MqjtbsNkDjd9xNUxZFLDXSDUY".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Funky Squirrel (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 2),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://Qmb8fm7CkzscjjoJGVp3p7qjSVMknsk27d3cwjqM26ELVB".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Weekend Ride (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 3),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmTwvwpgE9Fx6QZsjbXe5STHb3WVmaDuxFzafqCPueCmqc".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    amount = 1,
                    isStreamToken = false,
                    title = "Rave Culture (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 4),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmTETraR8WvExCaanc5aGT8EAUgCojyN8YSZYbGgmzVfja".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Vibrate (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 5),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://Qmdfr4PvuiZhi3a6EaDupGN6R33PKSy5kntwgFEzLQnPLR".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Top 40's (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 6),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmSp4Cn7qrhLTovezS1ii7ct1VAPK6Gotd2GnxnBc6ngSv".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Acid Trip (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 7),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmV8ihv8R6cCKsFJyFP8fhnnQjeKjS7HAAjmxMgUPftmw6".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "For The Win (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 8),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmWux5UpX6BtYQ7pjugqRh6ySa2vVJN12iSC2AB1cAQynU".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "Sunday Sermon (Master 2021)",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 9),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
            )

            val actualSongs = newmChainClient.queryNFTSongs(policyId, assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(9)
            assertThat(actualSongs).isEqualTo(expectedSongs)
        }

    @Test
    fun `Decentral Perk Lofi Beats, Legacy, Multiple`() =
        runBlocking {
            val policyId = "dee73575bbe4adb82b286fcb47aa9db4db5ae431209e64f19cc19c5b"
            val assetName = "DecentralPerkLofiBeats01576"
            val fingerprint = "asset10ze9rhc59n4xa8d8a5a4dr9gfm93k5utj9gl9f"
            val expectedSongs = listOf(
                NFTSong(
                    id = "ipfs://QmZ7Cd1y586YEC2uc7pUxzoCck2ZC9yyDtdMyWgKHGRfu6".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "1. doors open",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmZ7Cd1y586YEC2uc7pUxzoCck2ZC9yyDtdMyWgKHGRfu6",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmSzRkmegDyegknPzjVqvmCG35jmGTHJfpnQjJzN1mfh8r".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "2. cafè latte",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmSzRkmegDyegknPzjVqvmCG35jmGTHJfpnQjJzN1mfh8r",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmcsNULNnmjAxJspomPRZxJcQhh6YHXiMC53fvqHjkNkPg".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "3. windowpain",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmcsNULNnmjAxJspomPRZxJcQhh6YHXiMC53fvqHjkNkPg",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPL1v1PCBH42PaBkWbiFhF1Fc38vKnWAAw4q3V4X4w6os".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "4. ghost of the past",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmPL1v1PCBH42PaBkWbiFhF1Fc38vKnWAAw4q3V4X4w6os",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPHR3iu4j17V2zqonKDQzUU9BVqjox6RungWF7gdQeH6t".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "5. no fear in love",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmPHR3iu4j17V2zqonKDQzUU9BVqjox6RungWF7gdQeH6t",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmbwpXSZbmuLwbGsf3oARfYf269e4af6ZUY66wcQ3uB8uR".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "6. departing",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmbwpXSZbmuLwbGsf3oARfYf269e4af6ZUY66wcQ3uB8uR",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmZhux1Xqy46CNa7nCqKmpHuwR2nM7sJ6826WdRjhTTDgv".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "7. next stop",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmZhux1Xqy46CNa7nCqKmpHuwR2nM7sJ6826WdRjhTTDgv",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmcquduiNWU6EWJj8wLPSKcR8AiTEXvpcPTU4vwyX5VDfr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "8. soda-soda",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmcquduiNWU6EWJj8wLPSKcR8AiTEXvpcPTU4vwyX5VDfr",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPYgMQekJPWbvNhLP7QJAqUT2twjsBkRf32br6WPKL8dR".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "9. roadtrip",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmPYgMQekJPWbvNhLP7QJAqUT2twjsBkRf32br6WPKL8dR",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmQbSc3RYwuVG4qWKNbrQyrfcFXDZhFhAEt7VgBECC6HNN".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "10. cruisin' together",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmQbSc3RYwuVG4qWKNbrQyrfcFXDZhFhAEt7VgBECC6HNN",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmSSwxTVZdfTRe9HoCAr1EUjB9jzMrxNLV39LHnbgLDRYi".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "11. pass by the ville",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmSSwxTVZdfTRe9HoCAr1EUjB9jzMrxNLV39LHnbgLDRYi",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRkmwkfqi1KyaPFkFcHWQNXoB3DnmqJohTsQ7zyZU9PFb".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "12. downtown alley",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmRkmwkfqi1KyaPFkFcHWQNXoB3DnmqJohTsQ7zyZU9PFb",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmWRci2Ne5yyZ7ASV8NSFfeMnrTjy9cSe2uMKS1116Stf9".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "13. sunrise at the seaport",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmWRci2Ne5yyZ7ASV8NSFfeMnrTjy9cSe2uMKS1116Stf9",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmQQNTJDti9U4vv3ebKN5ACuu1spKzJCgodotwTWxdgdN5".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "14. morning thoughts",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmQQNTJDti9U4vv3ebKN5ACuu1spKzJCgodotwTWxdgdN5",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRCZxguhLVnYLYN253F6KgT86haFExjX4szq1Bo9dpP22".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "15. people come and go",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmRCZxguhLVnYLYN253F6KgT86haFExjX4szq1Bo9dpP22",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmTog3gpDC8aFCe7n8FWHsC3cSVFQbGDUkTUodeCL3BzC3".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "16. gonna make it",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmTog3gpDC8aFCe7n8FWHsC3cSVFQbGDUkTUodeCL3BzC3",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmUjJjzCh3FgoeJm5U1N2ixRX1Bc6tCSvCCdMgPS1PLAgr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "17. an old locksmith",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmUjJjzCh3FgoeJm5U1N2ixRX1Bc6tCSvCCdMgPS1PLAgr",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRHHihTiaoeEn1ZznkBfUKC2aNVTeKsA6kSioHARJv8TN".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "18. when they arrive",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmRHHihTiaoeEn1ZznkBfUKC2aNVTeKsA6kSioHARJv8TN",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmdSxVRRwPNXKfdn1ZxGfMD1tKFau53VfxZaqGWr6srBTr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "19. the penthouse",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/QmdSxVRRwPNXKfdn1ZxGfMD1tKFau53VfxZaqGWr6srBTr",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://Qmdjpq6sNRMsSApUzhy2YmiDmyUHuuaEYcpEHXjQp9ntdA".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "20. something beyond",
                    imageUrl = "https://ipfs.io/ipfs/QmNbo9zTojUGxPGB6vV75naJ2hsSrDK2jk2yLg7VU3YyGH",
                    audioUrl = "https://ipfs.io/ipfs/Qmdjpq6sNRMsSApUzhy2YmiDmyUHuuaEYcpEHXjQp9ntdA",
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                )
            )
            val actualSongs = newmChainClient.queryNFTSongs(policyId, assetName, false, false)
            assertThat(actualSongs.size).isEqualTo(20)
            assertThat(actualSongs).isEqualTo(expectedSongs)
        }

    @Test
    fun `Decentral Perk Lofi Beats, Legacy, NFTCDN`() =
        runBlocking {
            val nftCdnRepository: NftCdnRepository by inject()
            val policyId = "dee73575bbe4adb82b286fcb47aa9db4db5ae431209e64f19cc19c5b"
            val assetName = "DecentralPerkLofiBeats01576"
            val fingerprint = "asset10ze9rhc59n4xa8d8a5a4dr9gfm93k5utj9gl9f"
            val expectedSongs = listOf(
                NFTSong(
                    id = "ipfs://QmZ7Cd1y586YEC2uc7pUxzoCck2ZC9yyDtdMyWgKHGRfu6".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "1. doors open",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmSzRkmegDyegknPzjVqvmCG35jmGTHJfpnQjJzN1mfh8r".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "2. cafè latte",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 1),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmcsNULNnmjAxJspomPRZxJcQhh6YHXiMC53fvqHjkNkPg".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "3. windowpain",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 2),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPL1v1PCBH42PaBkWbiFhF1Fc38vKnWAAw4q3V4X4w6os".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "4. ghost of the past",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 3),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPHR3iu4j17V2zqonKDQzUU9BVqjox6RungWF7gdQeH6t".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "5. no fear in love",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 4),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmbwpXSZbmuLwbGsf3oARfYf269e4af6ZUY66wcQ3uB8uR".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "6. departing",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 5),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmZhux1Xqy46CNa7nCqKmpHuwR2nM7sJ6826WdRjhTTDgv".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "7. next stop",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 6),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmcquduiNWU6EWJj8wLPSKcR8AiTEXvpcPTU4vwyX5VDfr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "8. soda-soda",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 7),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmPYgMQekJPWbvNhLP7QJAqUT2twjsBkRf32br6WPKL8dR".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "9. roadtrip",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 8),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmQbSc3RYwuVG4qWKNbrQyrfcFXDZhFhAEt7VgBECC6HNN".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "10. cruisin' together",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 9),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmSSwxTVZdfTRe9HoCAr1EUjB9jzMrxNLV39LHnbgLDRYi".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "11. pass by the ville",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 10),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRkmwkfqi1KyaPFkFcHWQNXoB3DnmqJohTsQ7zyZU9PFb".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "12. downtown alley",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 11),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmWRci2Ne5yyZ7ASV8NSFfeMnrTjy9cSe2uMKS1116Stf9".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "13. sunrise at the seaport",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 12),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmQQNTJDti9U4vv3ebKN5ACuu1spKzJCgodotwTWxdgdN5".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "14. morning thoughts",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 13),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRCZxguhLVnYLYN253F6KgT86haFExjX4szq1Bo9dpP22".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "15. people come and go",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 14),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmTog3gpDC8aFCe7n8FWHsC3cSVFQbGDUkTUodeCL3BzC3".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "16. gonna make it",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 15),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmUjJjzCh3FgoeJm5U1N2ixRX1Bc6tCSvCCdMgPS1PLAgr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "17. an old locksmith",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 16),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmRHHihTiaoeEn1ZznkBfUKC2aNVTeKsA6kSioHARJv8TN".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "18. when they arrive",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 17),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://QmdSxVRRwPNXKfdn1ZxGfMD1tKFau53VfxZaqGWr6srBTr".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "19. the penthouse",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 18),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                ),
                NFTSong(
                    id = "ipfs://Qmdjpq6sNRMsSApUzhy2YmiDmyUHuuaEYcpEHXjQp9ntdA".toId(),
                    fingerprint = fingerprint,
                    policyId = policyId,
                    assetName = assetName,
                    isStreamToken = false,
                    amount = 1,
                    title = "20. something beyond",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 19),
                    duration = -1L,
                    artists = emptyList(),
                    genres = emptyList(),
                    moods = emptyList()
                )
            )

            val actualSongs = newmChainClient.queryNFTSongs(policyId, assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(20)
            assertThat(actualSongs).isEqualTo(expectedSongs)
        }

    private fun String.toId(): UUID = UUID.nameUUIDFromBytes(toByteArray())

    private suspend fun NewmChainCoroutineStub.queryNFTSongs(
        policyId: String,
        assetName: String,
        isStreamToken: Boolean,
        isNftCdnEnabled: Boolean
    ): List<NFTSong> {
        val assetNameHex = assetName.assetNameToHexString()
        val asset = NativeAsset.getDefaultInstance().copy {
            policy = policyId
            name = assetNameHex
            amount = "1"
        }
        return queryLedgerAssetMetadataListByNativeAsset(
            queryByNativeAssetRequest {
                policy = policyId
                name = assetNameHex
            }
        ).ledgerAssetMetadataList.toNFTSongs(asset, isStreamToken, isNftCdnEnabled)
    }

    private fun buildClient(): NewmChainCoroutineStub {
        val channel = ManagedChannelBuilder
            .forAddress(TEST_HOST, TEST_PORT)
            .apply {
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
