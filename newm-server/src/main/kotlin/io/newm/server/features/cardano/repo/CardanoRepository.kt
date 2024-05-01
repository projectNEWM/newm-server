package io.newm.server.features.cardano.repo

import com.google.protobuf.ByteString
import io.newm.chain.grpc.*
import io.newm.server.features.cardano.model.EncryptionRequest
import io.newm.server.features.cardano.model.GetWalletSongsResponse
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.NFTSong
import io.newm.server.typealiases.UserId
import kotlinx.coroutines.flow.Flow
import java.util.*

interface CardanoRepository {
    suspend fun saveKey(
        key: Key,
        name: String? = null
    ): UUID

    suspend fun isMainnet(): Boolean

    suspend fun getKey(keyId: UUID): Key

    suspend fun getKeyByName(name: String): Key?

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
        offset: Int,
        limit: Int
    ): GetWalletSongsResponse

    suspend fun getWalletNFTSongs(
        userId: UserId,
        includeLegacy: Boolean
    ): List<NFTSong>

    suspend fun getWalletImages(userId: UserId): List<String>

    // TODO: remove xpubKey support after client migrate to new Wallet Connection method
    suspend fun getWalletNFTSongs(
        xpubKey: String,
        includeLegacy: Boolean
    ): List<NFTSong>

    // TODO: remove xpubKey support after client migrate to new Wallet Connection method
    suspend fun getWalletImages(xpubKey: String): List<String>

    suspend fun snapshotToken(
        policyId: String,
        name: String
    ): SnapshotNativeAssetsResponse
}
