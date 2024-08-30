package io.newm.server.features.cardano.repo

import com.google.protobuf.ByteString
import io.newm.chain.grpc.CardanoEra
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.MonitorPaymentAddressResponse
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.SnapshotNativeAssetsResponse
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.VerifySignDataResponse
import io.newm.server.features.cardano.model.EncryptionRequest
import io.newm.server.features.cardano.model.GetWalletSongsResponse
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.NFTSong
import io.newm.server.features.song.model.SongFilters
import io.newm.server.typealiases.UserId
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CardanoRepository {
    suspend fun saveKey(
        key: Key,
        name: String? = null
    ): UUID

    suspend fun isMainnet(): Boolean

    suspend fun cardanoEra(): CardanoEra

    suspend fun getKey(keyId: UUID): Key

    suspend fun getKeyByName(name: String): Key?

    fun signTransaction(
        transactionIdBytes: ByteArray,
        signingKeys: List<Key>
    ): List<Signature>

    fun signTransactionDummy(signingKeys: List<Key>): List<Signature>

    suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse

    suspend fun submitTransaction(cborBytes: ByteString): SubmitTransactionResponse

    suspend fun awaitPayment(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse

    suspend fun queryLiveUtxos(address: String): List<Utxo>

    suspend fun queryPublicKeyHashByOutputRef(
        hash: String,
        ix: Long
    ): String

    suspend fun saveEncryptionParams(encryptionRequest: EncryptionRequest)

    suspend fun queryStreamTokenMinUtxo(): Long

    suspend fun queryAdaUSDPrice(): Long

    suspend fun queryNEWMUSDPrice(): Long

    suspend fun queryNativeTokenUSDPrice(
        policyId: String,
        assetName: String
    ): Long

    suspend fun isNewmToken(
        policyId: String,
        assetName: String
    ): Boolean

    suspend fun <T> withLock(block: suspend () -> T): T

    suspend fun verifySignData(
        signatureHex: String,
        publicKeyHex: String
    ): VerifySignDataResponse

    suspend fun verifySignTransaction(cborHex: String): VerifySignDataResponse

    suspend fun monitorAddress(
        address: String,
        startAfterTxId: String? = null
    ): Flow<MonitorAddressResponse>

    suspend fun getWalletSongs(
        request: List<String>,
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): GetWalletSongsResponse

    suspend fun getWalletNFTSongs(
        userId: UserId,
        includeLegacy: Boolean
    ): List<NFTSong>

    suspend fun getWalletImages(userId: UserId): List<String>

    suspend fun snapshotToken(
        policyId: String,
        name: String
    ): SnapshotNativeAssetsResponse

    /**
     * Get the first payment address on-chain where this stake address was used. This ensures that we never
     * use a franken-address where somebody tries to claim for a stake address that is not theirs.
     */
    suspend fun getReceiveAddressForStakeAddress(stakeAddress: String): String?

    /**
     * Save a script address to the whitelist
     */
    suspend fun saveScriptAddressToWhitelist(scriptAddress: String)

    companion object {
        const val MUTEX_NAME = "newm-server"

        // NEWM token information - mainnet
        const val NEWM_TOKEN_POLICY = "682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63"
        const val NEWM_TOKEN_NAME = "4e45574d"

        // NEWM token information - testnet
        const val NEWM_TOKEN_POLICY_TEST = "769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff"
        const val NEWM_TOKEN_NAME_TEST = "744e45574d"

        // Charli3 ADA/USD OracleFeed token
        const val CHARLI3_ADA_USD_POLICY = "08c56c0fa73748a23c3bc1d9e6a60a4187416fc4ff8fe3475506990e"
        const val CHARLI3_ADA_USD_NAME = "4f7261636c6546656564"

        // Charli3 ADA/USD OracleFeed token preprod
        const val CHARLI3_ADA_USD_POLICY_PREPROD = "1116903479e7320b8e4592207aaebf627898267fcd80e2d9646cbf07"
        const val CHARLI3_ADA_USD_NAME_PREPROD = "4f7261636c6546656564"

        // Charli3 NEWM/USD OracleFeed token
        const val CHARLI3_NEWM_USD_POLICY = "f155a26044efe91b3c44f87a7536d2d631c847717930ff547ae9d05c"
        const val CHARLI3_NEWM_USD_NAME = "4f7261636c6546656564"

        // Charli3 NEWM/USD OracleFeed token preprod
        const val CHARLI3_NEWM_USD_POLICY_PREPROD = "362e3f869c98ce971ead0e2705c56df467ddd2aecb44f6f216c3e1d5"
        const val CHARLI3_NEWM_USD_NAME_PREPROD = "4f7261636c6546656564"
    }
}
