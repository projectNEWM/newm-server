package io.newm.server.features.song.repo

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborTextString
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.util.toHexString
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.ReleaseTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongReceiptTable
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.client.HttpClient
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.UUID

class SongRepositoryImplTest {

    private lateinit var environment: ApplicationEnvironment
    private lateinit var configRepository: ConfigRepository
    private lateinit var cardanoRepository: CardanoRepository
    private lateinit var collaborationRepository: CollaborationRepository
    private lateinit var songRepository: SongRepositoryImpl

    @BeforeEach
    fun setup() {
        environment = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)
        cardanoRepository = mockk(relaxed = true)
        collaborationRepository = mockk(relaxed = true)

        val httpClient: HttpClient = mockk(relaxed = true)

        songRepository = spyk(SongRepositoryImpl(
            environment = environment,
            s3 = mockk(relaxed = true),
            configRepository = configRepository,
            cardanoRepository = cardanoRepository,
            distributionRepository = mockk(relaxed = true),
            collaborationRepository = collaborationRepository,
            emailRepository = mockk(relaxed = true),
            outletReleaseRepository = mockk(relaxed = true),
            httpClient = httpClient
        ))

        coEvery { songRepository.update(any<SongId>(), any<Song>(), any<UserId?>()) } just Runs
        coEvery { songRepository.updateSongMintingStatus(any(), any(), any()) } just Runs
        coEvery { songRepository.saveOrUpdateReceipt(any(), any()) } just Runs

        val db = Database.connect("jdbc:h2:mem:test_db_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction(db) {
            SchemaUtils.create(UserTable, ReleaseTable, SongTable, KeyTable, SongReceiptTable)
        }
    }

    @Test
    fun `test getMintingPaymentAmount with NEWM payment type`() = runBlocking {
        val songId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val collaborators = 2
        val mintCostBaseLovelace = 10000000L // 10 ADA
        val dspPriceUsdForAdaPaymentType = 2000000L // $2.00 USD cents for ADA
        val dspPriceUsdForNewmPaymentType = 1500000L // $1.50 USD cents for NEWM
        val minUtxoLovelacePerNft = 1500000L // Min ADA for each NFT (collab split)
        // Exchange rates (simulating BigInteger with 6 decimal places for USD values)
        val usdAdaExchangeRate = BigInteger("500000") // $0.50 per ADA
        val usdNewmExchangeRate = BigInteger("10000")   // $0.01 per NEWM

        coEvery { cardanoRepository.queryStreamTokenMinUtxo() } returns minUtxoLovelacePerNft
        coEvery { cardanoRepository.queryAdaUSDPrice() } returns usdAdaExchangeRate.toLong()
        coEvery { cardanoRepository.queryNEWMUSDPrice() } returns usdNewmExchangeRate.toLong()
        coEvery { configRepository.getLong(ConfigRepository.CONFIG_KEY_MINT_PRICE) } returns mintCostBaseLovelace
        coEvery { configRepository.getLong(ConfigRepository.CONFIG_KEY_DISTRIBUTION_PRICE_USD) } returns dspPriceUsdForAdaPaymentType
        coEvery { configRepository.getLong(ConfigRepository.CONFIG_KEY_DISTRIBUTION_PRICE_USD_NEWM) } returns dspPriceUsdForNewmPaymentType
        coEvery { collaborationRepository.getAllBySongId(songId) } returns List(collaborators) { mockk(relaxed = true) {
            every { royaltyRate } returns BigDecimal("50.0")
        }}
        coEvery { cardanoRepository.isMainnet() } returns false // Use preprod NEWM tokens

        transaction {
            UserEntity.new(requesterId) {}
            val releaseId = ReleaseEntity.new { ownerId = EntityID(requesterId, UserTable); title = "Test Release" }.id
            SongEntity.new(songId) { ownerId = EntityID(requesterId, UserTable); title = "Test Song"; this.releaseId = releaseId }
        }

        val response = songRepository.getMintingPaymentAmount(songId, requesterId, PaymentType.NEWM)

        assertNotNull(response.mintPaymentOptions)
        val newmOption = response.mintPaymentOptions.find { it.paymentType == PaymentType.NEWM }
        assertNotNull(newmOption)

        // Accurate expected NEWM price calculation based on SongRepositoryImpl.calculateMintPaymentResponse
        val sendTokenFeeLovelace = (collaborators * minUtxoLovelacePerNft)
        val totalMintCostInLovelace = mintCostBaseLovelace + sendTokenFeeLovelace

        val dspPriceNewmies = BigDecimal(dspPriceUsdForNewmPaymentType)
            .divide(usdNewmExchangeRate.toBigDecimal(), 6, RoundingMode.CEILING)
            .movePointRight(6) // To compare with BigInteger from response which has no decimals
            .toBigInteger()

        val newmiesEquivalentForMintCostLovelace = BigDecimal(totalMintCostInLovelace)
            .multiply(usdAdaExchangeRate.toBigDecimal())
            .divide(usdNewmExchangeRate.toBigDecimal(), 6, RoundingMode.CEILING)
            .toBigInteger()

        val expectedTotalNewmPrice = newmiesEquivalentForMintCostLovelace + dspPriceNewmies

        assertEquals(expectedTotalNewmPrice.toString(), BigDecimal(newmOption.price).movePointRight(6).toBigInteger().toString())

        // Verify cborHex structure for NEWM
        val changeAmountLovelace = 1000000L // 1 ADA, as used in calculateMintPaymentResponse
        val expectedCbor = CborArray.create(
            listOf(
                CborInteger.create(minUtxoLovelacePerNft + changeAmountLovelace),
                listOf(
                    nativeAsset {
                        policy = CardanoRepository.NEWM_TOKEN_POLICY_PREPROD
                        name = CardanoRepository.NEWM_TOKEN_NAME_PREPROD
                        amount = expectedTotalNewmPrice.toString()
                    }
                ).toNativeAssetCborMap()
            )
        ).toCborByteArray().toHexString()
        assertEquals(expectedCbor, newmOption.cborHex)

        val songUpdateSlot = slot<Song>()
        coVerify { songRepository.update(eq(songId), capture(songUpdateSlot), null) }
        assertEquals("NEWM", songUpdateSlot.captured.mintPaymentType)
        assertEquals(expectedTotalNewmPrice, songUpdateSlot.captured.mintCost?.toBigInteger())

        coVerify { songRepository.saveOrUpdateReceipt(songId, response) }
    }

    @Test
    fun `test generateMintingPaymentTransaction for NEWM payment`() = runBlocking {
        val songId = UUID.randomUUID()
        val releaseId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val paymentKeyId = UUID.randomUUID()
        val newmAmount = 12345L
        val minAdaForOutput = 1337000L
        val mockKey = Key(
            id = paymentKeyId,
            createdAt = mockk(),
            address = "addr_test_payment",
            vkey = "dummy_vkey",
            skey = "dummy_skey",
            policy = null,
            script = null,
            name = null
        )
        val dummyCborHex = "8200581cdeadbeef"

        // Setup DB entities
        transaction {
            UserEntity.new(ownerId) {}
            ReleaseEntity.new(releaseId) {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = "Test Release for NEWM Gen"
                this.mintPaymentType = "NEWM"
                this.mintCost = newmAmount
            }
            SongEntity.new(songId) {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = "Test Song for NEWM Gen"
                this.releaseId = EntityID(releaseId, ReleaseTable)
                this.mintPaymentType = "NEWM" // Ensure song.mintPaymentType is also set
                this.mintCost = newmAmount
            }
        }

        coEvery { songRepository.get(songId) } returns Song(
            id = songId,
            ownerId = ownerId,
            title = "Test Song for NEWM Gen",
            releaseId = releaseId,
            mintPaymentType = "NEWM",
            mintCost = newmAmount
        )
        coEvery { cardanoRepository.saveKey(any()) } returns paymentKeyId
        coEvery { cardanoRepository.isMainnet() } returns false // preprod
        coEvery { cardanoRepository.calculateMinUtxoForOutput(any()) } returns minAdaForOutput
        val buildTxSlot = slot<suspend TransactionBuilderResponse.Builder.() -> Unit>()
        coEvery { cardanoRepository.buildTransaction(capture(buildTxSlot)) } coAnswers {
            val builder = TransactionBuilderResponse.newBuilder()
            builder.transactionCbor = com.google.protobuf.ByteString.copyFromUtf8(dummyCborHex)
            // Simulate the builder logic correctly applying the captured lambda
            val dsl = TransactionBuilderResponse.Builder()
            buildTxSlot.captured.invoke(dsl) // this is a simplification
            builder.build()
        }


        val sourceUtxos = listOf(Utxo.newBuilder().apply { hash = "dummyhash"; ix = 0; lovelace = "5000000" }.build())
        val changeAddress = "addr_test_change"

        val resultCbor = songRepository.generateMintingPaymentTransaction(songId, ownerId, sourceUtxos, changeAddress)
        assertEquals(dummyCborHex, resultCbor)

        val outputUtxoSlot = slot<io.newm.chain.grpc.OutputUtxo>()
        coVerify { cardanoRepository.calculateMinUtxoForOutput(capture(outputUtxoSlot)) }

        val capturedCalcOutput = outputUtxoSlot.captured
        assertEquals(1, capturedCalcOutput.nativeAssetsCount)
        val nativeAsset = capturedCalcOutput.nativeAssetsList[0]
        assertEquals(CardanoRepository.NEWM_TOKEN_POLICY_PREPROD, nativeAsset.policy)
        assertEquals(CardanoRepository.NEWM_TOKEN_NAME_PREPROD, nativeAsset.name)
        assertEquals(newmAmount.toString(), nativeAsset.amount)
        // Lovelace in calculateMinUtxoForOutput's input might be 0 or unset initially depending on how it's built
        // The important part is that the *final* outputUtxo in buildTransaction has the correct lovelace

        val finalOutputUtxoList = mutableListOf<io.newm.chain.grpc.OutputUtxo>()
        coVerify {
            cardanoRepository.buildTransaction(coCapture { capturedBuilderLambda ->
                // To verify the outputUtxo passed to buildTransaction, we need to simulate its execution
                val mockBuilder = mockk<TransactionBuilderResponse.Builder>(relaxed = true) {
                    every { addOutputUtxos(any<io.newm.chain.grpc.OutputUtxo>()) } answers {
                        finalOutputUtxoList.add(firstArg())
                        this
                    }
                }
                runBlocking { capturedBuilderLambda.invoke(mockBuilder) }
            })
        }

        assertTrue(finalOutputUtxoList.isNotEmpty(), "Output UTXO list should not be empty")
        val finalOutput = finalOutputUtxoList.first()
        assertEquals(mockKey.address, finalOutput.address) // key.address is not directly available, using mockKey.address
        assertEquals(minAdaForOutput.toString(), finalOutput.lovelace)
        assertEquals(1, finalOutput.nativeAssetsCount)
        assertEquals(CardanoRepository.NEWM_TOKEN_POLICY_PREPROD, finalOutput.nativeAssetsList[0].policy)
        assertEquals(CardanoRepository.NEWM_TOKEN_NAME_PREPROD, finalOutput.nativeAssetsList[0].name)
        assertEquals(newmAmount.toString(), finalOutput.nativeAssetsList[0].amount)

        val songUpdateSlot = slot<Song>()
        coVerify { songRepository.update(eq(songId), capture(songUpdateSlot), null) }
        assertEquals(paymentKeyId, songUpdateSlot.captured.paymentKeyId)

        coVerify { songRepository.updateSongMintingStatus(songId, MintingStatus.MintingPaymentRequested, "") }
    }

    @Test
    fun `test refundMintingPayment for NEWM payment`() = runBlocking {
        val songId = UUID.randomUUID()
        val releaseId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val paymentKeyId = UUID.randomUUID()
        val newmAmountToRefund = 98765L
        val minAdaForRefundOutput = 1234000L
        val refundWalletAddress = "addr_test_refund_wallet"
        val txId = "test_tx_id_refund"

        val mockPaymentKey = Key(paymentKeyId, mockk(), "addr_test_payment_key", "vkey", "skey", null, null, "paymentKey")
        val mockCashRegisterKey = Key(UUID.randomUUID(), mockk(), "addr_test_cash_register", "vkey_cr", "skey_cr", null, null, "cashRegister")

        transaction {
            UserEntity.new(ownerId) {}
            ReleaseEntity.new(releaseId) {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = "Test Release for NEWM Refund"
                this.mintPaymentType = "NEWM"
                this.mintCost = newmAmountToRefund
            }
            SongEntity.new(songId) {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = "Test Song for NEWM Refund"
                this.releaseId = EntityID(releaseId, ReleaseTable)
                this.paymentKeyId = EntityID(paymentKeyId, KeyTable) // Set paymentKeyId for the song
            }
        }

        // Mocks for get() and getRelease()
         coEvery { songRepository.get(songId) } returns Song(
            id = songId,
            ownerId = ownerId,
            title = "Test Song for NEWM Refund",
            releaseId = releaseId,
            paymentKeyId = paymentKeyId // Ensure this is part of the returned Song object
        )
        coEvery { songRepository.getRelease(releaseId) } returns io.newm.server.features.song.model.Release(
            id = releaseId,
            ownerId = ownerId,
            title = "Test Release for NEWM Refund",
            mintPaymentType = "NEWM",
            mintCost = newmAmountToRefund
        )

        coEvery { cardanoRepository.getKey(paymentKeyId) } returns mockPaymentKey
        coEvery { cardanoRepository.getKeyByName("cashRegister") } returns mockCashRegisterKey
        coEvery { cardanoRepository.queryLiveUtxos(mockPaymentKey.address) } returns listOf(
            Utxo.newBuilder().apply { hash = "payment_utxo_hash"; ix = 0; lovelace = "2000000"; addNativeAssets(nativeAsset{policy="newm_policy"; name="newm_name"; amount=newmAmountToRefund.toString()}) }.build()
        )
        coEvery { cardanoRepository.queryLiveUtxos(mockCashRegisterKey.address) } returns listOf(
            Utxo.newBuilder().apply { hash = "cr_utxo_hash"; ix = 0; lovelace = "5000000" }.build()
        )
        coEvery { cardanoRepository.isMainnet() } returns false // preprod
        coEvery { cardanoRepository.calculateMinUtxoForOutput(any()) } returns minAdaForRefundOutput

        val buildTxSlotRefund = slot<suspend TransactionBuilderResponse.Builder.() -> Unit>()
        coEvery { cardanoRepository.buildTransaction(capture(buildTxSlotRefund)) } coAnswers {
            TransactionBuilderResponse.newBuilder().apply { transactionCbor = com.google.protobuf.ByteString.copyFromUtf8("dummy_refund_cbor") }.build()
        }
        coEvery { cardanoRepository.submitTransaction(any()) } returns SubmitTransactionResponse.newBuilder().apply { this.txId = txId; result = "MsgAcceptTx" }.build()

        val response = songRepository.refundMintingPayment(songId, refundWalletAddress)
        assertEquals(txId, response.transactionId)

        val finalRefundOutputList = mutableListOf<io.newm.chain.grpc.OutputUtxo>()
        coVerify {
            cardanoRepository.buildTransaction(coCapture { capturedBuilderLambda ->
                val mockBuilder = mockk<TransactionBuilderResponse.Builder>(relaxed = true) {
                    every { addOutputUtxos(any<io.newm.chain.grpc.OutputUtxo>()) } answers {
                        finalRefundOutputList.add(firstArg())
                        this
                    }
                }
                runBlocking { capturedBuilderLambda.invoke(mockBuilder) }
            })
        }

        assertTrue(finalRefundOutputList.isNotEmpty(), "Refund output UTXO list should not be empty")
        val finalRefundOutput = finalRefundOutputList.first { it.address == refundWalletAddress } // Ensure we check the correct output

        assertEquals(refundWalletAddress, finalRefundOutput.address)
        assertEquals(minAdaForRefundOutput.toString(), finalRefundOutput.lovelace)
        assertEquals(1, finalRefundOutput.nativeAssetsCount)
        val refundedNativeAsset = finalRefundOutput.nativeAssetsList[0]
        assertEquals(CardanoRepository.NEWM_TOKEN_POLICY_PREPROD, refundedNativeAsset.policy)
        assertEquals(CardanoRepository.NEWM_TOKEN_NAME_PREPROD, refundedNativeAsset.name)
        assertEquals(newmAmountToRefund.toString(), refundedNativeAsset.amount)

        val songUpdateSlot = slot<Song>()
        coVerify { songRepository.update(eq(songId), capture(songUpdateSlot), null) } // Assuming internal call to update
        assertEquals(MintingStatus.Declined, songUpdateSlot.captured.mintingStatus)
        assertEquals(txId, songUpdateSlot.captured.mintingTxId)
    }
}
