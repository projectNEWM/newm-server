package io.newm.server.features.cardano.repo

import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.MonitorPaymentAddressResponse
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.server.features.cardano.model.Key
import java.util.*

interface CardanoRepository {
    suspend fun add(key: Key): UUID
    suspend fun isMainnet(): Boolean
    suspend fun get(keyId: UUID): Key
    suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse
    suspend fun awaitPayment(request: MonitorPaymentAddressRequest): MonitorPaymentAddressResponse
}
