package io.newm.server.features.cardano.repo

import com.google.protobuf.ByteString
import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.MonitorPaymentAddressResponse
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.server.features.cardano.model.EncryptionRequest
import io.newm.server.features.cardano.model.GetWalletSongsResponse
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.NFTSong
import java.util.UUID

interface CardanoRepository {
    suspend fun saveKey(key: Key, name: String? = null): UUID
    suspend fun isMainnet(): Boolean
    suspend fun getKey(keyId: UUID): Key
    suspend fun getKeyByName(name: String): Key?
    suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse
    suspend fun submitTransaction(cborBytes: ByteString): SubmitTransactionResponse
    suspend fun awaitPayment(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse
    suspend fun queryLiveUtxos(address: String): List<Utxo>
    suspend fun queryPublicKeyHashByOutputRef(hash: String, ix: Long): String
    suspend fun saveEncryptionParams(encryptionRequest: EncryptionRequest)
    suspend fun queryStreamTokenMinUtxo(): Long
    suspend fun queryAdaUSDPrice(): Long
    suspend fun <T> withLock(block: suspend () -> T): T
    suspend fun getWalletSongs(request: List<String>, offset: Int, limit: Int): GetWalletSongsResponse
    suspend fun getWalletNFTSongs(xpubKey: String, includeLegacy: Boolean): List<NFTSong>
    suspend fun getWalletImages(xpubKey: String): List<String>
}
