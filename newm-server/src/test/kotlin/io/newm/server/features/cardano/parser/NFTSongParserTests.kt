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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
    fun `SickCity343, Legacy, Single`() =
        runBlocking {
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
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
            val expectedSong =
                NFTSong(
                    id = "ipfs://QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb".toId(),
                    fingerprint = "asset163r3tg4qggphswslxmfezu2gfevs35433pqz3e",
                    policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                    assetName = "SickCity344",
                    isStreamToken = false,
                    amount = 1,
                    title = "4EVR",
                    imageUrl = "https://ipfs.io/ipfs/QmSXPyRe9KzmY18R64pdzMyvMiacu1C8eosZWepw1Eexme",
                    audioUrl = "https://ipfs.io/ipfs/QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb",
                    duration = -1L,
                    artists = listOf("Irie Reyna"),
                    genres = listOf("R&B", "Soul"),
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
            val expectedSong =
                NFTSong(
                    id = "ipfs://QmY9LRJoKMgPbEc2hvvREWP7UBzYuZaqaWacAkp3HKFUzb".toId(),
                    fingerprint = fingerprint,
                    policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                    assetName = "SickCity344",
                    isStreamToken = false,
                    amount = 1,
                    title = "4EVR",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                    duration = -1L,
                    artists = listOf("Irie Reyna"),
                    genres = listOf("R&B", "Soul"),
                    moods = emptyList()
                )
            val actualSongs = newmChainClient.queryNFTSongs(expectedSong.policyId, expectedSong.assetName, false, true)
            assertThat(actualSongs.size).isEqualTo(1)
            assertThat(actualSongs.first()).isEqualTo(expectedSong)
        }

    @Test
    fun `SickCity349, Legacy, Single`() =
        runBlocking {
            val expectedSong =
                NFTSong(
                    id = "ipfs://QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr".toId(),
                    fingerprint = "asset1n8jsja05t2dlsy3e7wzjcr2a8724qmgmnwv669",
                    policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                    assetName = "SickCity349",
                    isStreamToken = false,
                    amount = 1,
                    title = "You, I, and The Ocean",
                    imageUrl = "https://ipfs.io/ipfs/QmdZtxeKLGTXGkpcWkWznCZW7qsyiGA78dFcDwy9cA4D1d",
                    audioUrl = "https://ipfs.io/ipfs/QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr",
                    duration = -1L,
                    artists = listOf("Sam Katman"),
                    genres = listOf("Singer-Songwriter", "Folk Pop"),
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
            val expectedSong =
                NFTSong(
                    id = "ipfs://QmczfeP54gZgjMVnbe2mLrBjQbkQu3zuA1zYKpgSVzzKBr".toId(),
                    fingerprint = fingerprint,
                    policyId = "123da5e4ef337161779c6729d2acd765f7a33a833b2a21a063ef65a5",
                    assetName = "SickCity349",
                    isStreamToken = false,
                    amount = 1,
                    title = "You, I, and The Ocean",
                    imageUrl = nftCdnRepository.generateImageUrl(fingerprint),
                    audioUrl = nftCdnRepository.generateFileUrl(fingerprint, 0),
                    duration = -1L,
                    artists = listOf("Sam Katman"),
                    genres = listOf("Singer-Songwriter", "Folk Pop"),
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
            val expectedSongs =
                listOf(
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
            val expectedSongs =
                listOf(
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

    private fun String.toId(): UUID = UUID.nameUUIDFromBytes(toByteArray())

    private suspend fun NewmChainCoroutineStub.queryNFTSongs(
        policyId: String,
        assetName: String,
        isStreamToken: Boolean,
        isNftCdnEnabled: Boolean
    ): List<NFTSong> {
        val assetNameHex = assetName.assetNameToHexString()
        val asset =
            NativeAsset.getDefaultInstance().copy {
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
        val channel =
            ManagedChannelBuilder
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
