package io.newm.server.features.cardano.repo

import io.newm.server.features.cardano.model.EncryptionRequest
import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.MonitorPaymentAddressResponse
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.server.features.cardano.model.Key
import java.util.UUID

interface CardanoRepository {
    suspend fun saveKey(key: Key, name: String? = null): UUID
    suspend fun isMainnet(): Boolean
    suspend fun getKey(keyId: UUID): Key
    suspend fun getKeyByName(name: String): Key?
    suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse
    suspend fun awaitPayment(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse
    suspend fun queryLiveUtxos(address: String): List<Utxo>
    suspend fun saveEncryptionParams(encryptionRequest: EncryptionRequest)
}
