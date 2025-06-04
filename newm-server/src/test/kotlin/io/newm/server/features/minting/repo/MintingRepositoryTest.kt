package io.newm.server.features.minting.repo

import com.firehose.model.CliKey
import com.firehose.model.CliKeyPair
import com.google.common.truth.Truth.assertThat
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.newm.chain.grpc.NewmChainGrpcKt
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.utxo
import io.newm.chain.util.hexToByteArray
import io.newm.server.BaseApplicationTests
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.cardano.repo.CardanoRepositoryImpl
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.collaboration.repo.CollaborationRepositoryImpl
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.model.FilterCriteria
import io.newm.server.typealiases.SongId
import io.newm.shared.ktx.toHexString
import io.newm.txbuilder.ktx.toCborObject
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertTrue


class MintingRepositoryTest : BaseApplicationTests() {

    private lateinit var userRepository: UserRepository
    private lateinit var collaborationRepository: CollaborationRepository
    private lateinit var cardanoRepository: CardanoRepository
    private lateinit var configRepository: ConfigRepository
    private lateinit var songRepository: SongRepository // Mocked for MintingRepositoryImpl dependencies
    private lateinit var mintingRepository: MintingRepositoryImpl

    // Test constants
    private val ownerId = UUID.randomUUID()
    private var testSongId = UUID.randomUUID() // Changed to var for multiple test setups
    private var testReleaseId = UUID.randomUUID()
    private var testPaymentKeyId = UUID.randomUUID()

    private val expectedCip68PolicyId = "test_cip68_policy_id"


    @BeforeEach
    fun beforeEach() {
        transaction {
            UserTable.deleteAll()
            // Add other tables if created by original setupDatabase or addSongToDatabase
            // Example: ReleaseTable.deleteAll(), SongTable.deleteAll(), CollaborationTable.deleteAll()
        }
        userRepository = mockk(relaxed = true)
        collaborationRepository = mockk(relaxed = true)
        cardanoRepository = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)
        songRepository = mockk(relaxed = true)

        mintingRepository = MintingRepositoryImpl(
            userRepository,
            collaborationRepository,
            cardanoRepository,
            configRepository
        )

        // Common mocks for all tests, can be overridden in specific tests
        val owner = User(id = ownerId, firstName = "Test", lastName = "User", email = "owner@email.com", walletAddress = "owner_wallet_address")
        coEvery { userRepository.get(ownerId) } returns owner
        coEvery { userRepository.findByEmail(any()) } returns owner // Simplified for collaborators

        val collaboration = Collaboration(email = "collab@email.com", royaltyRate = BigDecimal("100.0"), status = CollaborationStatus.Accepted)
        coEvery { collaborationRepository.getAllBySongId(any()) } returns listOf(collaboration) // Use any() for songId flexibility

        // Common Cardano key mocks
        val paymentKeyMock = Key(id = testPaymentKeyId, address = "payment_key_address", skey = "skey", vkey = "vkey", createdAt = LocalDateTime.now())
        val cashRegisterKeyMock = Key(id = UUID.randomUUID(), address = "cash_register_address", skey = "skey_cr", vkey = "vkey_cr", createdAt = LocalDateTime.now(), name = "cashRegister")
        val collateralKeyMock = Key(id = UUID.randomUUID(), address = "collateral_address", skey = "skey_coll", vkey = "vkey_coll", createdAt = LocalDateTime.now(), name = "collateral")
        coEvery { cardanoRepository.getKey(testPaymentKeyId) } returns paymentKeyMock
        coEvery { cardanoRepository.getKeyByName("cashRegister") } returns cashRegisterKeyMock
        coEvery { cardanoRepository.getKeyByName("collateral") } returns collateralKeyMock
        coEvery { cardanoRepository.getKeyByName("moneyBox") } returns null // Default to no moneyBox

        // Common ConfigRepository mocks
        coEvery { configRepository.getString(ConfigRepository.CONFIG_KEY_MINT_CIP68_SCRIPT_ADDRESS) } returns "cip68_script_address"
        coEvery { configRepository.getString(ConfigRepository.CONFIG_KEY_MINT_CIP68_POLICY) } returns expectedCip68PolicyId
        coEvery { configRepository.getLong(ConfigRepository.CONFIG_KEY_MINT_CASH_REGISTER_COLLECTION_AMOUNT) } returns 20000000L
        coEvery { configRepository.getLong(ConfigRepository.CONFIG_KEY_MINT_CASH_REGISTER_MIN_AMOUNT) } returns 5000000L
        coEvery { configRepository.getString(ConfigRepository.CONFIG_KEY_MINT_STARTER_TOKEN_UTXO_REFERENCE) } returns "starter_ref_hash#0"
        coEvery { configRepository.getString(ConfigRepository.CONFIG_KEY_MINT_SCRIPT_UTXO_REFERENCE) } returns "script_ref_hash#1"
        coEvery { cardanoRepository.isMainnet() } returns false // Default to preprod for NEWM tokens

        // Common Cardano transaction mocks
        val dummySignatures = listOf(Signature.newBuilder().setSignature("dummySig").build())
        coEvery { cardanoRepository.signTransactionDummy(any()) } returns dummySignatures
        coEvery { cardanoRepository.signTransaction(any(), any()) } returns dummySignatures
        val txBuilderResponse = TransactionBuilderResponse.newBuilder()
            .setTransactionId("test_tx_id")
            .setTransactionCbor(com.google.protobuf.ByteString.copyFromUtf8("dummy_tx_cbor"))
            .build()
        coEvery { cardanoRepository.buildTransaction(any()) } returns txBuilderResponse
        coEvery { cardanoRepository.submitTransaction(any()) } returns SubmitTransactionResponse.newBuilder()
            .setResult("MsgAcceptTx")
            .setTxId("test_tx_id")
            .build()
    }

    private fun createMockSong(mintCostLovelace: Long? = null): Song {
        testSongId = UUID.randomUUID() // Ensure unique songId for each test creating a song
        return Song(
            id = testSongId,
            ownerId = ownerId,
            title = "Test Song",
            releaseId = testReleaseId,
            paymentKeyId = testPaymentKeyId,
            arweaveLyricsUrl = "ar://lyrics",
            arweaveTokenAgreementUrl = "ar://agreement",
            arweaveClipUrl = "ar://clip",
            duration = 200000,
            genres = listOf("Electronic"),
            isrc = "QZ123",
            moods = listOf("Energetic"),
            compositionCopyrightOwner = "Owner",
            compositionCopyrightYear = 2024,
            phonographicCopyrightOwner = "Owner",
            phonographicCopyrightYear = 2024,
            mintCostLovelace = mintCostLovelace
        )
    }

    private fun mockReleaseAndSong(
        mintPaymentType: String?,
        mintCost: Long?,
        songMintCostLovelace: Long?
    ) {
        testReleaseId = UUID.randomUUID() // Ensure unique releaseId
        val release = Release(
            id = testReleaseId,
            ownerId = ownerId,
            title = "Test Release",
            releaseType = ReleaseType.SINGLE,
            arweaveCoverArtUrl = "ar://cover",
            releaseDate = LocalDate.now(),
            publicationDate = LocalDate.now(),
            mintPaymentType = mintPaymentType,
            mintCost = mintCost
        )
        coEvery { songRepository.getRelease(testReleaseId) } returns release
        // The song object passed to mint() will be created by createMockSong() in each test
    }

    private fun mockCommonUtxosForMint() {
        val cashRegisterUtxos = listOf(utxo { hash = "cr_utxo_1"; ix = 0; lovelace = "10000000" })
        coEvery { cardanoRepository.queryLiveUtxos(cardanoRepository.getKeyByName("cashRegister")!!.address) } returns cashRegisterUtxos

        val collateralUtxo = utxo { hash = "coll_utxo_1"; ix = 0; lovelace = "5000000" }
        coEvery { cardanoRepository.queryLiveUtxos(cardanoRepository.getKeyByName("collateral")!!.address) } returns listOf(collateralUtxo)
    }

    @Test
    fun `test mint with NEWM payment`() = runBlocking {
        val newmPaymentAmount = 10000L
        mockReleaseAndSong(mintPaymentType = "NEWM", mintCost = newmPaymentAmount, songMintCostLovelace = null)
        val songToMint = createMockSong(mintCostLovelace = null)
        mockCommonUtxosForMint()

        val newmPolicy = CardanoRepository.NEWM_TOKEN_POLICY_PREPROD
        val newmName = CardanoRepository.NEWM_TOKEN_NAME_PREPROD
        val newmPaymentUtxo = utxo {
            hash = "payment_utxo_hash_newm"
            ix = 0
            lovelace = "2000000" // 2 ADA
            nativeAssets.add(nativeAsset { policy = newmPolicy; name = newmName; amount = newmPaymentAmount.toString() })
        }
        coEvery { cardanoRepository.queryLiveUtxos(cardanoRepository.getKey(testPaymentKeyId)!!.address) } returns listOf(newmPaymentUtxo)

        val mintInfo = mintingRepository.mint(songToMint)

        assertThat(mintInfo.policyId).isEqualTo(expectedCip68PolicyId)
        val cashRegisterUtxos = cardanoRepository.queryLiveUtxos(cardanoRepository.getKeyByName("cashRegister")!!.address)
        val expectedRefUtxo = (cashRegisterUtxos + listOf(newmPaymentUtxo)).sortedBy { it.hash + "#" + it.ix }.first()
        val (_, calculatedFracTokenName) = mintingRepository.calculateTokenNames(expectedRefUtxo)
        assertThat(mintInfo.assetName).isEqualTo(calculatedFracTokenName)

        val buildTxSlot = slot<suspend TransactionBuilderResponse.Builder.() -> Unit>()
        coVerify(exactly = 2) { cardanoRepository.buildTransaction(capture(buildTxSlot)) }
        val capturedSourceUtxos = mutableListOf<Utxo>()
        val mockTxBuilderForCapture = mockk<TransactionBuilderResponse.Builder>(relaxed = true) {
            every { addSourceUtxos(any<Utxo>()) } answers { capturedSourceUtxos.add(firstArg()); this }
            every { addAllSourceUtxos(any<Iterable<Utxo>>()) } answers { capturedSourceUtxos.addAll(firstArg()); this }
        }
        runBlocking { buildTxSlot.captured.invoke(mockTxBuilderForCapture) }
        assertTrue(capturedSourceUtxos.any { it.hash == newmPaymentUtxo.hash && it.ix == newmPaymentUtxo.ix }, "NEWM payment UTXO missing.")
        coVerify { cardanoRepository.submitTransaction(any()) }
    }

    @Test
    fun `test mint with Modern ADA payment`() = runBlocking {
        val adaPaymentAmount = 5000000L // 5 ADA
        mockReleaseAndSong(mintPaymentType = "ADA", mintCost = adaPaymentAmount, songMintCostLovelace = null) // songMintCostLovelace can be anything, release.mintCost takes precedence
        val songToMint = createMockSong(mintCostLovelace = null) // song.mintCostLovelace is not used due to release.mintPaymentType being "ADA"
        mockCommonUtxosForMint()

        val adaPaymentUtxo = utxo { hash = "payment_utxo_hash_ada"; ix = 0; lovelace = adaPaymentAmount.toString() }
        coEvery { cardanoRepository.queryLiveUtxos(cardanoRepository.getKey(testPaymentKeyId)!!.address) } returns listOf(adaPaymentUtxo)

        val mintInfo = mintingRepository.mint(songToMint)

        assertThat(mintInfo.policyId).isEqualTo(expectedCip68PolicyId)
        val cashRegisterUtxos = cardanoRepository.queryLiveUtxos(cardanoRepository.getKeyByName("cashRegister")!!.address)
        val expectedRefUtxo = (cashRegisterUtxos + listOf(adaPaymentUtxo)).sortedBy { it.hash + "#" + it.ix }.first()
        val (_, calculatedFracTokenName) = mintingRepository.calculateTokenNames(expectedRefUtxo)
        assertThat(mintInfo.assetName).isEqualTo(calculatedFracTokenName)

        val buildTxSlot = slot<suspend TransactionBuilderResponse.Builder.() -> Unit>()
        coVerify(exactly = 2) { cardanoRepository.buildTransaction(capture(buildTxSlot)) }
        val capturedSourceUtxos = mutableListOf<Utxo>()
        val mockTxBuilderForCapture = mockk<TransactionBuilderResponse.Builder>(relaxed = true) {
            every { addSourceUtxos(any<Utxo>()) } answers { capturedSourceUtxos.add(firstArg()); this }
            every { addAllSourceUtxos(any<Iterable<Utxo>>()) } answers { capturedSourceUtxos.addAll(firstArg()); this }
        }
        runBlocking { buildTxSlot.captured.invoke(mockTxBuilderForCapture) }
        assertTrue(capturedSourceUtxos.any { it.hash == adaPaymentUtxo.hash && it.ix == adaPaymentUtxo.ix && it.lovelace == adaPaymentAmount.toString() }, "ADA payment UTXO missing or incorrect.")
        coVerify { cardanoRepository.submitTransaction(any()) }
    }

    @Test
    fun `test mint with Legacy ADA payment`() = runBlocking {
        val legacyAdaPaymentAmount = 6000000L // 6 ADA
        mockReleaseAndSong(mintPaymentType = null, mintCost = null, songMintCostLovelace = legacyAdaPaymentAmount)
        val songToMint = createMockSong(mintCostLovelace = legacyAdaPaymentAmount) // song.mintCostLovelace is used here
        mockCommonUtxosForMint()

        val legacyAdaPaymentUtxo = utxo { hash = "payment_utxo_hash_legacy_ada"; ix = 0; lovelace = legacyAdaPaymentAmount.toString() }
        coEvery { cardanoRepository.queryLiveUtxos(cardanoRepository.getKey(testPaymentKeyId)!!.address) } returns listOf(legacyAdaPaymentUtxo)

        val mintInfo = mintingRepository.mint(songToMint)

        assertThat(mintInfo.policyId).isEqualTo(expectedCip68PolicyId)
        val cashRegisterUtxos = cardanoRepository.queryLiveUtxos(cardanoRepository.getKeyByName("cashRegister")!!.address)
        val expectedRefUtxo = (cashRegisterUtxos + listOf(legacyAdaPaymentUtxo)).sortedBy { it.hash + "#" + it.ix }.first()
        val (_, calculatedFracTokenName) = mintingRepository.calculateTokenNames(expectedRefUtxo)
        assertThat(mintInfo.assetName).isEqualTo(calculatedFracTokenName)

        val buildTxSlot = slot<suspend TransactionBuilderResponse.Builder.() -> Unit>()
        coVerify(exactly = 2) { cardanoRepository.buildTransaction(capture(buildTxSlot)) }
        val capturedSourceUtxos = mutableListOf<Utxo>()
        val mockTxBuilderForCapture = mockk<TransactionBuilderResponse.Builder>(relaxed = true) {
            every { addSourceUtxos(any<Utxo>()) } answers { capturedSourceUtxos.add(firstArg()); this }
            every { addAllSourceUtxos(any<Iterable<Utxo>>()) } answers { capturedSourceUtxos.addAll(firstArg()); this }
        }
        runBlocking { buildTxSlot.captured.invoke(mockTxBuilderForCapture) }
        assertTrue(capturedSourceUtxos.any { it.hash == legacyAdaPaymentUtxo.hash && it.ix == legacyAdaPaymentUtxo.ix && it.lovelace == legacyAdaPaymentAmount.toString() }, "Legacy ADA payment UTXO missing or incorrect.")
        coVerify { cardanoRepository.submitTransaction(any()) }
    }

    // Original tests from MintingRepositoryTest.kt are manually merged below this line

    private lateinit var originalSetupSongId: SongId // To avoid conflict with testSongId for new tests

    private suspend fun originalSetupDatabase() { // Renamed to avoid conflict
        val primaryArtistId =
            listOf(
                User(
                    firstName = "Danketsu",
                    lastName = "",
                    genre = "House",
                    role = "Artist",
                    email = "danketsu@me.com",
                    nickname = "Danketsu",
                    websiteUrl = "https://danketsu.io",
                    instagramUrl = "https://instagram.com/danketsu",
                    twitterUrl = "https://twitter.com/danketsu",
                ),
                User(
                    firstName = "Mirai",
                    lastName = "Music",
                    genre = "House",
                    role = "Artist",
                    email = "mirai@me.com",
                    nickname = "Mirai Music",
                    websiteUrl = "https://miraimusicproductions.com",
                ),
                User(
                    firstName = "Ashley",
                    lastName = "Nastasia Griffin",
                    genre = "House",
                    role = "Artist",
                    email = "NSTASIA@me.com",
                    nickname = "NSTASIA",
                    websiteUrl = "https://www.nstasia.com",
                    twitterUrl = "https://twitter.com/nstasia",
                ),
                User(
                    firstName = "Bob",
                    lastName = "Ross",
                    role = "Artwork",
                    email = "bob.ross@pbs.org",
                ),
                User(
                    firstName = "Aaron",
                    lastName = "Ortiz",
                    role = "Author (Lyrics)",
                    email = "lyrics1@domain.com",
                ),
                User(
                    firstName = "Aled",
                    lastName = "Williams",
                    role = "Author (Lyrics)",
                    email = "lyrics2@domain.com",
                ),
                User(
                    firstName = "Kahmeil",
                    lastName = "Brown",
                    role = "Author (Lyrics)",
                    email = "lyrics3@domain.com",
                ),
                User(
                    firstName = "Elijah",
                    lastName = "Boac",
                    role = "Author (Lyrics)",
                    email = "lyrics4@domain.com",
                ),
                User(
                    firstName = "Tommy",
                    lastName = "Bui",
                    role = "Author (Lyrics)",
                    email = "lyrics5@domain.com",
                ),
                User(
                    firstName = "Kyle",
                    lastName = "Kim",
                    role = "Author (Lyrics)",
                    email = "lyrics6@domain.com",
                ),
                User(
                    firstName = "Riccardo",
                    lastName = "Lovatto",
                    role = "Author (Lyrics)",
                    email = "lyrics7@domain.com",
                ),
                User(
                    firstName = "Jeff",
                    lastName = "MacMillan",
                    role = "Synthesizer",
                    email = "jeff@macmillan.io",
                )
            ).map { user -> addUserToDatabase(user) }.first()

        originalSetupSongId =
            addSongToDatabase(
                Release(
                    ownerId = primaryArtistId,
                    title = "Daisuke",
                    releaseType = ReleaseType.SINGLE,
                    arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                    releaseDate = LocalDate.parse("2023-02-03"),
                    publicationDate = LocalDate.parse("2023-02-03"),
                ),
                Song(
                    ownerId = primaryArtistId,
                    title = "Daisuke",
                    genres = listOf("Pop", "House", "Tribal"),
                    isrc = "QZ-NW7-23-57511",
                    moods = listOf("spiritual"),
                    arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                    arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
                    arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                    duration = 200000,
                    track = 1,
                    compositionCopyrightOwner = "Mirai Music Publishing",
                    compositionCopyrightYear = 2023,
                    phonographicCopyrightOwner = "Danketsu, Mirai Music, NSTASIA",
                    phonographicCopyrightYear = 2023,
                    mintingStatus = MintingStatus.Pending
                )
            )

        listOf(
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "bob.ross@pbs.org", roles = listOf("Artwork"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "NSTASIA@me.com", roles = listOf("Artist"), royaltyRate = 10.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "mirai@me.com", roles = listOf("Artist"), royaltyRate = 10.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics1@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics2@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics3@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics4@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics5@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics6@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "lyrics7@domain.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "NSTASIA@me.com", roles = listOf("Author (Lyrics)"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "jeff@macmillan.io", roles = listOf("Synthesizer"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
            Collaboration( id = UUID.randomUUID(), createdAt = LocalDateTime.now(), songId = originalSetupSongId, email = "mirai@me.com", roles = listOf("Producer"), royaltyRate = 0.0f.toBigDecimal(), credited = true, status = CollaborationStatus.Accepted, ),
        ).forEach { collab -> addCollabToDatabase(collab) }
    }

    @Test
    fun `buildStreamTokenMetadata test`(): Unit =
        runBlocking {
            originalSetupDatabase() // Uses the original setup method

            val localSongRepository: SongRepository = SongRepositoryImpl(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
            val localCollabRepository: CollaborationRepository = CollaborationRepositoryImpl(mockk(), mockk())
            // Use the class member mintingRepository which is already set up with mocks
            // Or, if this test needs specific non-mocked instances for some parts:
            val tempMintingRepo = MintingRepositoryImpl(userRepository, localCollabRepository, cardanoRepository, configRepository)


            val song = localSongRepository.get(originalSetupSongId) // Use localSongRepository for DB interaction
            val release = localSongRepository.getRelease(song.releaseId!!)
            val primaryArtist = transaction { UserEntity.getByEmail("danketsu@me.com")!!.toModel(false) }
            val collabs = localCollabRepository.getAll(
                primaryArtist.id!!,
                CollaborationFilters(
                    inbound = null,
                    songIds = FilterCriteria(includes = listOf(song.id!!)),
                    olderThan = null, newerThan = null, ids = null,
                    statuses = FilterCriteria(includes = listOf(CollaborationStatus.Accepted)),
                ),
                0, Integer.MAX_VALUE
            )

            val plutusDataHex = tempMintingRepo.buildStreamTokenMetadata(release, song, primaryArtist, collabs)
                .toCborObject().toCborByteArray().toHexString()

            println("plutusDataHex: $plutusDataHex")
            assertThat(plutusDataHex).isEqualTo(
                "d87982a6446e616d654744616973756b6545696d616765583061723a2f2f476c4d6c714849506a7755746c5055665178446458316a57536a6c4b4b314243544249656b586741363641496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a64c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654744616973756b654c72656c656173655f646174654a323032332d30322d3033507075626c69636174696f6e5f646174654a323032332d30322d30334b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4d76697375616c5f61727469737448426f6220526f73734566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f654b3867415043764a2d396b626950335072534d774c47416b3338614e7978504475647a7a624779707845a4446e616d654744616973756b65496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f5170676a6d576d41484e65525667785f596c7776683136693361576438424267797137663136676155753044736f6e67ac4a736f6e675f7469746c654744616973756b654d736f6e675f6475726174696f6e475054334d3230534c747261636b5f6e756d62657201446d6f6f644973706972697475616c476172746973747382a1446e616d65474e535441534941a1446e616d654b4d69726169204d757369634667656e7265738343506f7045486f7573654654726962616c49636f707972696768745f5840c2a92032303233204d69726169204d75736963205075626c697368696e672c20e2849720323032332044616e6b657473752c204d69726169204d757369632c20474e535441534941ff466c7972696373583061723a2f2f3776515448546b6779626e386e564c446c756b476942617a79324e5a5668575036485a644a646d5048303044697372634f515a2d4e57372d32332d3537353131496c7972696369737473884b4161726f6e204f7274697a4d416c65642057696c6c69616d734d4b61686d65696c2042726f776e4b456c696a616820426f616349546f6d6d7920427569484b796c65204b696d50526963636172646f204c6f766174746f474e53544153494154636f6e747269627574696e675f6172746973747381581b4a656666204d61634d696c6c616e2c2053796e74686573697a65724870726f64756365724b4d69726169204d7573696301"
            )
        }

    @Test
    fun `test calculateTokenNames`() {
        val (refTokenName, fracTokenName) =
            mintingRepository.calculateTokenNames( // Uses class member mintingRepository
                utxo {
                    hash = "1e637fd4b1a6a633261a1ba463577d65209dbbe0f7e8ec1fbfedb4c6b1bb926b"
                    ix = 1
                }
            )
        assertThat(refTokenName).isEqualTo("000643b00138c741df813afd1e2ba521d6b798dcabbc813ac7ba84467080b9b6")
        assertThat(fracTokenName).isEqualTo("001bc2800138c741df813afd1e2ba521d6b798dcabbc813ac7ba84467080b9b6")

        val (identityRefTokenName, identityFracTokenName) =
            mintingRepository.calculateTokenNames( // Uses class member mintingRepository
                utxo {
                    hash = ""
                    ix = 0
                }
            )
        assertThat(identityRefTokenName).isEqualTo("000643b000a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a")
        assertThat(identityFracTokenName).isEqualTo("001bc28000a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a")
    }

    @Test
    @Disabled("This test requires a live gRPC connection and valid JWT")
    fun `test buildMintingTransaction`(): Unit =
        runBlocking {
            originalSetupDatabase() // Uses the original setup method

            val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
            val client = NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Metadata().apply {
                        put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>")
                    }
                )
            )

            val liveCardanoRepository = CardanoRepositoryImpl(client, mockk(), "", mockk(), mockk(), mockk())
            val localSongRepository: SongRepository = SongRepositoryImpl(mockk(), mockk(), mockk(), liveCardanoRepository, mockk(), mockk(), mockk(), mockk(), mockk())
            val localCollabRepository: CollaborationRepository = CollaborationRepositoryImpl(mockk(), mockk())
            val tempMintingRepo = MintingRepositoryImpl(userRepository, localCollabRepository, liveCardanoRepository, configRepository)


            val song = localSongRepository.get(originalSetupSongId)
            val release = localSongRepository.getRelease(song.releaseId!!)
            val primaryArtist = transaction { UserEntity.getByEmail("danketsu@me.com")!!.toModel(false) }
            val collabs = localCollabRepository.getAll(
                primaryArtist.id!!,
                CollaborationFilters(
                    inbound = null, songIds = FilterCriteria(includes = listOf(song.id!!)),
                    olderThan = null, newerThan = null, ids = null,
                    statuses = FilterCriteria(includes = listOf(CollaborationStatus.Accepted)),
                ),
                0, Integer.MAX_VALUE
            )

            val cip68Metadata = tempMintingRepo.buildStreamTokenMetadata(release, song, primaryArtist, collabs)
            val cashRegisterUtxos = listOf(
                utxo { hash = "13814410996dd1ec8b8c9beeb7169204622eb3af8e8111287ac66e086607692f"; ix = 0L; lovelace = "8000000" },
                utxo { hash = "89ca570459a08619d6b8872d8d32271dabbaa0b98baed642a347bbdd2a21460e"; ix = 0L; lovelace = "2000000" }
            )
            val (refTokenName, fracTokenName) = tempMintingRepo.calculateTokenNames(cashRegisterUtxos.first())
            val starterTokenUtxoReference = utxo { hash = "1e5fcbbce6a2f83aa343fb6eb8767a35c5f0b63b36c53373a343be36fc39502f"; ix = 0; lovelace = "2366190" }
            val mintScriptUtxoReference = utxo { hash = "2363c9d42f8d7e4f33b208c43af098cee1ce7457e7a4d789fd0e100367926a6e"; ix = 1; lovelace = "13658390" }

            val paymentKey = Key.createFromCliKeys(CliKeyPair("payment", vkey = CliKey(type = "PaymentVerificationKeyShelley_ed25519", description = "Payment Verification Key", cborHex = "5820839a69bfbc9a8041199558194cdf904bf0ccabae75f9129bc3bdf936960ae221")))
            val cashRegisterKey = Key.createFromCliKeys(CliKeyPair("newm", vkey = CliKey(type = "PaymentVerificationKeyShelley_ed25519", description = "Payment Verification Key", cborHex = "5820dd121dfde327b2e4fa13b3bb870360bfa8b92ad6f3dd8afd3e0b7e0447a42974")))
            val collateralKey = Key.createFromCliKeys(CliKeyPair("collateral", vkey = CliKey(type = "PaymentVerificationKeyShelley_ed25519", description = "Payment Verification Key", cborHex = "582030fe8d8ec967ed402263cc6f9bfa20e6a90963d0529edb1bf9c5bec07ff22f2f")))
            val signingKeys = listOf(cashRegisterKey, paymentKey, collateralKey)

            var response = tempMintingRepo.buildMintingTransaction(
                paymentUtxo = utxo { hash = "8ca645eee53117971504b47712f86105ce84f9d8ebc2985ea7ce0a815846a603"; ix = 0L; lovelace = "10000000" },
                cashRegisterUtxos = cashRegisterUtxos,
                changeAddress = "addr_test1vr620zal7m270eyjj9vcd27yj5uzy3a0vkgps3g6yh8vjtq702l28",
                moneyBoxUtxos = null, moneyBoxAddress = null, cashRegisterCollectionAmount = 10000000L,
                collateralUtxo = utxo { hash = "1964b50dc9ad31b339d5a82d9f579cd9e7d91053b97e2ca27b4aa8a5b88edb40"; ix = 2L; lovelace = "5000000" },
                collateralReturnAddress = "addr_test1vqfrhfv4hghz50yna9syvxjrltg3gjgf4k998e0ulp0y73qpp79gs",
                cip68ScriptAddress = "addr_test1xr2nxf8mm2qeswmhqsg3t4jvp3u67qndmvt004jy953u8xmwp6udvtz8wsdxfar962t9ea4eesmylnr8llndpm0n76ps4wgc7l",
                cip68Metadata = cip68Metadata,
                cip68Policy = "a0488f6ef1b8b5b268583312a94aaebefb36e570b198e02024d321a9",
                refTokenName = refTokenName, fracTokenName = fracTokenName,
                streamTokenSplits = listOf(Pair("addr_test1vz0pdhl9yagp6mk4ncqvgxx796sl4hxarfazy88s63xtdscuqxqf5", 100000000L)),
                requiredSigners = signingKeys,
                starterTokenUtxoReference = starterTokenUtxoReference,
                mintScriptUtxoReference = mintScriptUtxoReference,
                signatures = liveCardanoRepository.signTransactionDummy(signingKeys)
            )
            println("transactionId: ${response.transactionId}")
            val transactionIdBytes = response.transactionId.hexToByteArray()
            response = tempMintingRepo.buildMintingTransaction(
                paymentUtxo = utxo { hash = "8ca645eee53117971504b47712f86105ce84f9d8ebc2985ea7ce0a815846a603"; ix = 0L; lovelace = "10000000" },
                cashRegisterUtxos = cashRegisterUtxos,
                changeAddress = "addr_test1vr620zal7m270eyjj9vcd27yj5uzy3a0vkgps3g6yh8vjtq702l28",
                moneyBoxUtxos = null, moneyBoxAddress = null, cashRegisterCollectionAmount = 10000000L,
                collateralUtxo = utxo { hash = "1964b50dc9ad31b339d5a82d9f579cd9e7d91053b97e2ca27b4aa8a5b88edb40"; ix = 2L; lovelace = "5000000" },
                collateralReturnAddress = "addr_test1vqfrhfv4hghz50yna9syvxjrltg3gjgf4k998e0ulp0y73qpp79gs",
                cip68ScriptAddress = "addr_test1xr2nxf8mm2qeswmhqsg3t4jvp3u67qndmvt004jy953u8xmwp6udvtz8wsdxfar962t9ea4eesmylnr8llndpm0n76ps4wgc7l",
                cip68Metadata = cip68Metadata,
                cip68Policy = "a0488f6ef1b8b5b268583312a94aaebefb36e570b198e02024d321a9",
                refTokenName = refTokenName, fracTokenName = fracTokenName,
                streamTokenSplits = listOf(Pair("addr_test1vz0pdhl9yagp6mk4ncqvgxx796sl4hxarfazy88s63xtdscuqxqf5", 100000000L)),
                requiredSigners = signingKeys,
                starterTokenUtxoReference = starterTokenUtxoReference,
                mintScriptUtxoReference = mintScriptUtxoReference,
                signatures = liveCardanoRepository.signTransaction(transactionIdBytes, signingKeys),
            )
            println("transactionId: ${response.transactionId}")
            val cborSigned = response.transactionCbor
            println("cborSigned: ${cborSigned.toByteArray().toHexString()}")
            val submitTransactionResponse = liveCardanoRepository.submitTransaction(cborSigned)
            println("submitTransactionResponse: $submitTransactionResponse")
        }
}
