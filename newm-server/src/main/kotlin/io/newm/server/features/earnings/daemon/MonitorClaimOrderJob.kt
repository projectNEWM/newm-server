package io.newm.server.features.earnings.daemon

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborTextString
import com.google.protobuf.ByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.hexToByteArray
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_NAME
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_NAME_TEST
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_POLICY
import io.newm.server.features.cardano.repo.CardanoRepository.Companion.NEWM_TOKEN_POLICY_TEST
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.ClaimOrderStatus
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.toUUID
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

class MonitorClaimOrderJob : Job {
    private val log = KotlinLogging.logger {}
    private val cardanoRepository: CardanoRepository by inject()
    private val earningsRepository: EarningsRepository by inject()
    private val configRepository: ConfigRepository by inject()

    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        log.info {
            "MonitorClaimOrderJob key: ${context.jobDetail.key.name} started at ${
                LocalDateTime.ofInstant(
                    context.fireTime.toInstant(),
                    ZoneOffset.UTC
                )
            }"
        }
        runBlocking {
            val claimOrderId = context.mergedJobDataMap.getString("claimOrderId").toUUID()
            val isMainnet = cardanoRepository.isMainnet()
            val newmTokenPolicy = if (isMainnet) NEWM_TOKEN_POLICY else NEWM_TOKEN_POLICY_TEST
            val newmTokenName = if (isMainnet) NEWM_TOKEN_NAME else NEWM_TOKEN_NAME_TEST
            val claimOrder = earningsRepository.getByClaimOrderId(claimOrderId)
            try {
                if (claimOrder != null) {
                    val response =
                        cardanoRepository.awaitPayment(
                            monitorPaymentAddressRequest {
                                address = claimOrder.paymentAddress
                                lovelace = claimOrder.paymentAmount.toString()
                                timeoutMs =
                                    configRepository
                                        .getLong(
                                            CONFIG_KEY_EARNINGS_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
                                        ).minutes.inWholeMilliseconds
                            }
                        )
                    if (response.success) {
                        log.info { "Payment received for claim order ${claimOrder.id}" }
                        val receiveAddress = cardanoRepository.getReceiveAddressForStakeAddress(claimOrder.stakeAddress)
                        if (receiveAddress == null) {
                            val errorMessage = "Receive address not found for stake address ${claimOrder.stakeAddress}"
                            log.error { errorMessage }
                            earningsRepository.update(
                                claimOrder.copy(
                                    status = ClaimOrderStatus.Failed,
                                    errorMessage = errorMessage
                                )
                            )
                            return@runBlocking
                        }

                        val cashRegisterKey =
                            requireNotNull(cardanoRepository.getKeyByName("cashRegister")) { "cashRegister key not defined!" }
                        val paymentKey = cardanoRepository.getKey(claimOrder.keyId)
                        val paymentUtxo =
                            cardanoRepository.queryLiveUtxos(paymentKey.address).first {
                                it.lovelace.toLong() == claimOrder.paymentAmount && it.nativeAssetsCount == 0
                            }

                        // get the fee paid for the incoming paymentUtxo transaction. We will want to reimburse it
                        // to the user in the claim transaction
                        val txFeeToReimburseLovelace = 200_000L // 0.2 ADA
                        // TODO: have newm chain record each transaction fee in the DB
                        // Then reimburse the user the actual fee paid for the transaction or 0.2 ADA if the tx
                        // fee is larger than that
                        // cardanoRepository.getTxFee(paymentUtxo.hash)

                        val earningsWalletKey =
                            requireNotNull(cardanoRepository.getKeyByName("earningsWallet")) { "earningsWallet key not found" }

                        val earningsToClaim = earningsRepository.getAllByIds(claimOrder.earningsIds)
                        val newmAmountToClaim = earningsToClaim.sumOf { it.amount.toBigInteger() }

                        cardanoRepository.withLock {
                            val cashRegisterUtxos =
                                cardanoRepository
                                    .queryLiveUtxos(cashRegisterKey.address)
                                    .filter { it.nativeAssetsCount == 0 }
                                    .sortedByDescending { it.lovelace.toLong() }
                                    .take(5)
                            require(cashRegisterUtxos.isNotEmpty()) { "cashRegister has no utxos!" }

                            val earningsUtxos =
                                cardanoRepository
                                    .queryLiveUtxos(earningsWalletKey.address)
                                    .filter {
                                        it.nativeAssetsCount > 0 &&
                                            it.nativeAssetsList.all { asset ->
                                                asset.policy == newmTokenPolicy && asset.name == newmTokenName
                                            }
                                    }.take(5)

                            val sourceNewmTokenAmount =
                                earningsUtxos.sumOf { utxo ->
                                    utxo.nativeAssetsList.sumOf { asset -> asset.amount.toBigInteger() }
                                }
                            require(sourceNewmTokenAmount >= newmAmountToClaim) {
                                "Not enough NEWM tokens in the earningsWallet to claim!"
                            }

                            val signingKeys = listOf(paymentKey, earningsWalletKey, cashRegisterKey)
                            var signatures = cardanoRepository.signTransactionDummy(signingKeys)

                            var transactionBuilderResponse =
                                buildClaimTransaction(
                                    paymentUtxo,
                                    earningsUtxos,
                                    cashRegisterUtxos,
                                    receiveAddress,
                                    claimOrder,
                                    txFeeToReimburseLovelace,
                                    newmTokenPolicy,
                                    newmTokenName,
                                    newmAmountToClaim,
                                    earningsWalletKey,
                                    sourceNewmTokenAmount,
                                    cashRegisterKey,
                                    signatures
                                )

                            // check for errors in tx building
                            if (transactionBuilderResponse.hasErrorMessage()) {
                                val errorMessage =
                                    "TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}"
                                log.error { errorMessage }
                                earningsRepository.update(
                                    claimOrder.copy(
                                        status = ClaimOrderStatus.Failed,
                                        errorMessage = errorMessage
                                    )
                                )
                                return@withLock
                            }

                            val transactionIdBytes = transactionBuilderResponse.transactionId.hexToByteArray()
                            signatures = cardanoRepository.signTransaction(transactionIdBytes, signingKeys)

                            // sign with the actual signatures
                            transactionBuilderResponse =
                                buildClaimTransaction(
                                    paymentUtxo,
                                    earningsUtxos,
                                    cashRegisterUtxos,
                                    receiveAddress,
                                    claimOrder,
                                    txFeeToReimburseLovelace,
                                    newmTokenPolicy,
                                    newmTokenName,
                                    newmAmountToClaim,
                                    earningsWalletKey,
                                    sourceNewmTokenAmount,
                                    cashRegisterKey,
                                    signatures
                                )

                            // check for errors in tx building
                            if (transactionBuilderResponse.hasErrorMessage()) {
                                val errorMessage =
                                    "TransactionBuilder Error!: ${transactionBuilderResponse.errorMessage}"
                                log.error { errorMessage }
                                earningsRepository.update(
                                    claimOrder.copy(
                                        status = ClaimOrderStatus.Failed,
                                        errorMessage = errorMessage
                                    )
                                )
                                return@withLock
                            }

                            // Submit the transaction to Cardano
                            val submitTransactionResponse =
                                cardanoRepository.submitTransaction(transactionBuilderResponse.transactionCbor)
                            if (submitTransactionResponse.result == "MsgAcceptTx") {
                                newSuspendedTransaction {
                                    earningsRepository.claimed(
                                        claimOrderId = claimOrder.id!!,
                                        earningsIds = claimOrder.earningsIds
                                    )
                                    earningsRepository.update(
                                        claimOrder.copy(
                                            status = ClaimOrderStatus.Completed,
                                            transactionId = transactionBuilderResponse.transactionId
                                        )
                                    )
                                }
                                log.info {
                                    "Claim order ${claimOrder.id} completed successfully -> ${
                                        newmAmountToClaim.toBigDecimal(
                                            scale = 6
                                        )
                                    } NEWM, txid: ${transactionBuilderResponse.transactionId}"
                                }
                            } else {
                                val errorMessage =
                                    "Transaction submit failed with error: ${submitTransactionResponse.result}"
                                log.error { errorMessage }
                                earningsRepository.update(
                                    claimOrder.copy(
                                        status = ClaimOrderStatus.Failed,
                                        errorMessage = errorMessage
                                    )
                                )
                            }
                        }
                    } else {
                        log.info { "Payment timed out for claim order ${claimOrder.id}" }
                        earningsRepository.update(claimOrder.copy(status = ClaimOrderStatus.Timeout))
                    }
                } else {
                    log.error { "Claim order not found for id $claimOrderId" }
                }
            } catch (e: Throwable) {
                if (e is StatusException && e.status == Status.UNAVAILABLE) {
                    log.warn { "Claim order for id: $claimOrderId - ${e.message}" }
                    claimOrder?.let {
                        earningsRepository.update(claimOrder.copy(status = ClaimOrderStatus.Timeout))
                    }
                } else {
                    log.error(e) { "Error in MonitorClaimOrderJob" }
                    // re-schedule this job by throwing since requestRecovery is true
                    delay(60_000L)
                    throw JobExecutionException(e, true)
                }
            }
        }
    }

    private suspend fun buildClaimTransaction(
        paymentUtxo: Utxo,
        earningsUtxos: List<Utxo>,
        cashRegisterUtxos: List<Utxo>,
        receiveAddress: String,
        claimOrder: ClaimOrder,
        txFeeToReimburseLovelace: Long,
        newmTokenPolicy: String,
        newmTokenName: String,
        newmAmountToClaim: BigInteger,
        earningsWalletKey: Key,
        sourceNewmTokenAmount: BigInteger,
        cashRegisterKey: Key,
        signatures: List<Signature>
    ) = cardanoRepository.buildTransaction {
        with(sourceUtxos) {
            add(paymentUtxo)
            addAll(earningsUtxos)
            addAll(cashRegisterUtxos)
        }

        with(outputUtxos) {
            // NEWM to the receiveAddress for the claim
            add(
                outputUtxo {
                    address = receiveAddress
                    lovelace = (claimOrder.paymentAmount + txFeeToReimburseLovelace).toString()
                    nativeAssets.add(
                        nativeAsset {
                            policy = newmTokenPolicy
                            name = newmTokenName
                            amount = newmAmountToClaim.toString()
                        }
                    )
                }
            )
            // NEWM Change back to earningsWallet
            add(
                outputUtxo {
                    address = earningsWalletKey.address
                    // lovelace = "0" auto-calculated minutxo
                    nativeAssets.add(
                        nativeAsset {
                            policy = newmTokenPolicy
                            name = newmTokenName
                            amount = (sourceNewmTokenAmount - newmAmountToClaim).toString()
                        }
                    )
                }
            )
        }

        // Extra ada (if any) to cashRegister
        changeAddress = cashRegisterKey.address

        transactionMetadataCbor =
            ByteString.copyFrom(
                CborMap
                    .create(
                        mapOf(
                            CborInteger.create(674) to
                                CborMap.create(
                                    mapOf(
                                        CborTextString.create("msg") to
                                            CborArray.create().apply {
                                                add(CborTextString.create("NEWM Claim"))
                                            }
                                    )
                                )
                        )
                    ).toCborByteArray()
            )

        // sign the transaction
        if (signatures.isNotEmpty()) {
            this.signatures.addAll(signatures)
        }
    }
}
