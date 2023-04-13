package io.newm.server.features.cardano.repo

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kms.AWSKMSAsync
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.DecryptResult
import com.amazonaws.services.kms.model.EncryptRequest
import com.amazonaws.services.kms.model.EncryptResult
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.network.util.DefaultByteBufferPool
import io.newm.chain.grpc.IsMainnetRequest
import io.newm.chain.grpc.MonitorPaymentAddressRequest
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.chain.grpc.TransactionBuilderRequestKt
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.util.b64ToByteArray
import io.newm.chain.util.toB64String
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.model.QueryCurrentProtocolBabbageParametersResult
import io.newm.server.features.cardano.database.KeyEntity
import io.newm.server.features.cardano.model.Key
import io.newm.shared.ktx.debug
import io.newm.shared.koin.inject
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.time.Duration
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class CardanoRepositoryImpl(
    private val client: NewmChainCoroutineStub,
    private val kms: AWSKMSAsync,
    private val kmsKeyId: String,
) : CardanoRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    private var _isMainnet: Boolean? = null

    override suspend fun add(key: Key): UUID {
        logger.debug { "add: key = $key" }
        val eSkey = encryptSkey(key.skey)
        return transaction {
            KeyEntity.new {
                address = key.address
                skey = eSkey
                vkey = key.vkey.toHexString()
                script = key.script
                scriptAddress = key.scriptAddress
            }.id.value
        }
    }

    override suspend fun get(keyId: UUID): Key {
        logger.debug { "get: keyId = $keyId" }
        val keyEntity = transaction {
            KeyEntity[keyId]
        }
        return keyEntity.toModel(decryptSkey(keyEntity.skey))
    }

    override suspend fun isMainnet(): Boolean {
        if (_isMainnet == null) {
            // Check with newm-chain to see whether we're connected to a mainnet or a testnet
            _isMainnet = client.isMainnet(IsMainnetRequest.getDefaultInstance()).isMainnet
        }

        return _isMainnet!!
    }

    private val currentEpochCache = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build<Int, Long>()
    private val protocolParametersCache = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<Long, QueryCurrentProtocolBabbageParametersResult>()

    override suspend fun buildTransaction(block: TransactionBuilderRequestKt.Dsl.() -> Unit): TransactionBuilderResponse {
        val request = io.newm.chain.grpc.transactionBuilderRequest {
            block.invoke(this)
        }
        return client.transactionBuilder(request)
    }

    override suspend fun awaitPayment(request: MonitorPaymentAddressRequest) = client.monitorPaymentAddress(request)

    private suspend fun encryptSkey(skey: ByteArray): String {
        val plaintextBuffer = DefaultByteBufferPool.borrow()
        try {
            plaintextBuffer.put(skey)
            plaintextBuffer.flip()
            return suspendCoroutine { continuation ->
                kms.encryptAsync(
                    EncryptRequest().withKeyId(kmsKeyId).withPlaintext(plaintextBuffer),
                    object : AsyncHandler<EncryptRequest, EncryptResult> {
                        override fun onError(exception: Exception) {
                            continuation.resumeWithException(exception)
                        }

                        override fun onSuccess(request: EncryptRequest, result: EncryptResult) {
                            val ciphertextBuffer = result.ciphertextBlob.asReadOnlyBuffer()
                            val ciphertextBytes = ByteArray(ciphertextBuffer.remaining())
                            ciphertextBuffer.get(ciphertextBytes)
                            continuation.resume(ciphertextBytes.toB64String())
                        }
                    }
                )
            }
        } finally {
            DefaultByteBufferPool.recycle(plaintextBuffer)
        }
    }

    private suspend fun decryptSkey(b64Skey: String): ByteArray {
        val ciphertextBuffer = DefaultByteBufferPool.borrow()
        try {
            ciphertextBuffer.put(b64Skey.b64ToByteArray())
            ciphertextBuffer.flip()

            return suspendCoroutine { continuation ->
                kms.decryptAsync(
                    DecryptRequest().withKeyId(kmsKeyId).withCiphertextBlob(ciphertextBuffer),
                    object : AsyncHandler<DecryptRequest, DecryptResult> {
                        override fun onError(exception: Exception) {
                            continuation.resumeWithException(exception)
                        }

                        override fun onSuccess(request: DecryptRequest, result: DecryptResult) {
                            val plaintextBuffer = result.plaintext.asReadOnlyBuffer()
                            val plaintext = ByteArray(plaintextBuffer.remaining())
                            plaintextBuffer.get(plaintext)
                            continuation.resume(plaintext)
                        }
                    }
                )
            }
        } finally {
            DefaultByteBufferPool.recycle(ciphertextBuffer)
        }
    }
}
