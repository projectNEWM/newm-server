package io.newm.server.features.marketplace.repo

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toAdaString
import io.newm.chain.util.toHexString
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_MIN_INCENTIVE_AMOUNT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NFTCDN_ENABLED
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.marketplace.builders.buildQueueDatum
import io.newm.server.features.marketplace.database.MarketplaceArtistEntity
import io.newm.server.features.marketplace.database.MarketplaceBookmarkEntity
import io.newm.server.features.marketplace.database.MarketplacePendingOrderEntity
import io.newm.server.features.marketplace.database.MarketplacePurchaseEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.marketplace.model.OrderAmountRequest
import io.newm.server.features.marketplace.model.OrderAmountResponse
import io.newm.server.features.marketplace.model.OrderTransactionRequest
import io.newm.server.features.marketplace.model.OrderTransactionResponse
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.marketplace.parser.parseQueue
import io.newm.server.features.marketplace.parser.parseSale
import io.newm.server.features.song.database.SongTable
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpPaymentRequiredException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.coLazy
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.epochSecondsToLocalDateTime
import io.newm.shared.ktx.getConfigLong
import io.newm.shared.ktx.info
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.warn
import io.newm.txbuilder.ktx.mergeAmounts
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.math.BigInteger
import java.util.UUID

private const val SALE_TIP_KEY = "saleTip"
private const val QUEUE_TIP_KEY = "queueTip"

internal class MarketplaceRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository
) : MarketplaceRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val queueContractAddress: String by coLazy {
        environment.getSecureConfigString("marketplace.queue.contractAddress")
    }
    private val pointerPolicyId: String by coLazy {
        environment.getSecureConfigString("marketplace.pointerPolicyId")
    }
    private val incentivePolicyId: String by coLazy {
        environment.getSecureConfigString("marketplace.incentive.policyId")
    }
    private val incentiveAssetName: String by coLazy {
        environment.getSecureConfigString("marketplace.incentive.assetName")
    }
    private val pendingOrderTimeToLive: Long by lazy {
        environment.getConfigLong("marketplace.pendingOrderTimeToLive")
    }

    override suspend fun getSale(saleId: UUID): Sale {
        log.debug { "getSale: saleId = $saleId" }
        val sale = transaction { MarketplaceSaleEntity[saleId] }
        val isMainnet = cardanoRepository.isMainnet()
        val isNftCdnEnabled = configRepository.getBoolean(CONFIG_KEY_NFTCDN_ENABLED)
        val costAmountUsd = sale.getCostAmountUsd()
        return transaction { sale.toModel(isMainnet, isNftCdnEnabled, costAmountUsd) }
    }

    override suspend fun getSales(
        filters: SaleFilters,
        offset: Int,
        limit: Int
    ): List<Sale> {
        log.debug { "getSales: filters = $filters, offset = $offset, limit = $limit" }
        val isMainnet = cardanoRepository.isMainnet()
        val isNftCdnEnabled = configRepository.getBoolean(CONFIG_KEY_NFTCDN_ENABLED)
        val sales =
            transaction {
                MarketplaceSaleEntity.all(filters).limit(n = limit, offset = offset.toLong()).toList()
            }
        val costAmountsUsd: Map<UUID, String> = sales.associate { it.id.value to it.getCostAmountUsd() }
        return transaction {
            sales.map { it.toModel(isMainnet, isNftCdnEnabled, costAmountsUsd[it.id.value]!!) }
        }
    }

    override suspend fun getSaleCount(filters: SaleFilters): Long {
        log.debug { "getSaleCount: filters = $filters" }
        return transaction {
            MarketplaceSaleEntity.all(filters).count()
        }
    }

    override suspend fun getArtist(artistId: UserId): Artist {
        log.debug { "getArtist: artistId = $artistId" }
        return transaction {
            MarketplaceArtistEntity[artistId].toModel()
        }
    }

    override suspend fun getArtists(
        filters: ArtistFilters,
        offset: Int,
        limit: Int
    ): List<Artist> {
        log.debug { "getArtists: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            MarketplaceArtistEntity.all(filters)
                .limit(n = limit, offset = offset.toLong())
                .map(MarketplaceArtistEntity::toModel)
        }
    }

    override suspend fun getArtistCount(filters: ArtistFilters): Long {
        log.debug { "getArtistCount: filters = $filters" }
        return transaction {
            MarketplaceArtistEntity.all(filters).count()
        }
    }

    override suspend fun generateOrderAmount(request: OrderAmountRequest): OrderAmountResponse {
        log.debug { "generateOrderAmount: $request" }

        val sale = transaction { MarketplaceSaleEntity[request.saleId] }
        require(request.bundleQuantity <= sale.availableBundleQuantity) {
            "Bundle quantity (${request.bundleQuantity}) exceeds sale availability (${sale.availableBundleQuantity})"
        }

        val minIncentiveAmount = configRepository.getLong(CONFIG_KEY_MARKETPLACE_MIN_INCENTIVE_AMOUNT)
        val incentiveAmount =
            request.incentiveAmount?.also {
                require(it >= minIncentiveAmount) {
                    "Incentive amount ($it) is less than minimum required ($minIncentiveAmount)"
                }
            } ?: minIncentiveAmount

        val nativeAssets =
            listOf(
                nativeAsset {
                    policy = sale.costPolicyId
                    name = sale.costAssetName
                    amount = (sale.costAmount.toBigInteger() * request.bundleQuantity.toBigInteger()).toString()
                },
                nativeAsset {
                    policy = incentivePolicyId
                    name = incentiveAssetName
                    amount = (incentiveAmount.toBigInteger() * BigInteger.TWO).toString()
                }
            )

        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE)
        val amount =
            CborArray.create(
                listOf(
                    CborInteger.create(lovelace.toBigInteger()),
                    nativeAssets.toNativeAssetCborMap()
                )
            )

        val order =
            transaction {
                MarketplacePendingOrderEntity.deleteAllExpired(pendingOrderTimeToLive)
                MarketplacePendingOrderEntity.new {
                    saleId = sale.id
                    bundleQuantity = request.bundleQuantity
                    this.incentiveAmount = incentiveAmount
                }
            }

        return OrderAmountResponse(
            orderId = order.id.value,
            amountCborHex = amount.toCborByteArray().toHexString()
        )
    }

    override suspend fun generateOrderTransaction(request: OrderTransactionRequest): OrderTransactionResponse {
        log.debug { "generateOrderTransaction: $request" }

        if (request.utxos.isEmpty()) {
            throw HttpPaymentRequiredException("Missing UTXO's")
        }

        val (order, sale) =
            transaction {
                MarketplacePendingOrderEntity.deleteAllExpired(pendingOrderTimeToLive)
                MarketplacePendingOrderEntity[request.orderId].let { it to MarketplaceSaleEntity[it.saleId] }
            }

        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE)

        val transaction =
            cardanoRepository.buildTransaction {
                sourceUtxos.addAll(request.utxos)
                outputUtxos.add(
                    outputUtxo {
                        address = queueContractAddress
                        this.lovelace = lovelace.toString()
                        nativeAssets.addAll(
                            listOf(
                                nativeAsset {
                                    policy = sale.costPolicyId
                                    name = sale.costAssetName
                                    amount =
                                        (sale.costAmount.toBigInteger() * order.bundleQuantity.toBigInteger()).toString()
                                },
                                nativeAsset {
                                    policy = incentivePolicyId
                                    name = incentiveAssetName
                                    amount = (order.incentiveAmount.toBigInteger() * BigInteger.TWO).toString()
                                }
                            ).mergeAmounts()
                        )
                        datum =
                            buildQueueDatum(
                                ownerAddress = sale.ownerAddress,
                                numberOfBundles = order.bundleQuantity,
                                incentivePolicyId = incentivePolicyId,
                                incentiveAssetName = incentiveAssetName,
                                incentiveAmount = order.incentiveAmount,
                                pointerAssetName = sale.pointerAssetName
                            ).toCborObject().toCborByteArray().toHexString()
                    }
                )
                changeAddress = request.changeAddress
            }
        if (transaction.hasErrorMessage()) {
            throw HttpUnprocessableEntityException("Failed to build order transaction: ${transaction.errorMessage}")
        }
        transaction {
            order.delete()
        }
        return OrderTransactionResponse(
            transaction.transactionCbor.toByteArray().toHexString()
        )
    }

    override suspend fun getSaleTransactionTip(): String? {
        log.debug { "getSaleTransactionTip" }
        return transaction {
            MarketplaceBookmarkEntity.findById(SALE_TIP_KEY)?.txId
        }
    }

    override suspend fun getQueueTransactionTip(): String? {
        log.debug { "getQueueTransactionTip" }
        return transaction {
            MarketplaceBookmarkEntity.findById(QUEUE_TIP_KEY)?.txId
        }
    }

    override suspend fun processSaleTransaction(response: MonitorAddressResponse) {
        log.info { "Processing sale transaction ${response.txId}, block ${response.block}, slot ${response.slot}" }

        val isMainnet = cardanoRepository.isMainnet()
        val input = response.spentUtxosList.firstOrNull()?.parseSale(isMainnet)
        val output = response.createdUtxosList.firstOrNull()?.parseSale(isMainnet)
        val inputPointer = input?.getToken(pointerPolicyId)
        val outputPointer = output?.getToken(pointerPolicyId)

        when {
            input == null && output != null && outputPointer == null -> {
                log.info { "Detected NEW SALE (not started yet): $output" }
            }

            inputPointer == null && outputPointer != null -> {
                log.info { "Detected SALE START: $input" }
                val found =
                    transaction {
                        val songId = output.bundle.getMatchingSongId() ?: return@transaction false
                        MarketplaceSaleEntity.new {
                            createdAt = response.timestamp.epochSecondsToLocalDateTime()
                            status = SaleStatus.Started
                            this.songId = songId
                            ownerAddress = output.ownerAddress
                            pointerPolicyId = outputPointer.policyId
                            pointerAssetName = outputPointer.assetName
                            bundlePolicyId = output.bundle.policyId
                            bundleAssetName = output.bundle.assetName
                            bundleAmount = output.bundle.amount
                            costPolicyId = output.cost.policyId
                            costAssetName = output.cost.assetName
                            costAmount = output.cost.amount
                            maxBundleSize = output.maxBundleSize
                            totalBundleQuantity = output.bundleQuantity
                            availableBundleQuantity = output.bundleQuantity
                        }
                        true
                    }
                if (!found) {
                    log.warn { "Detected SALE START, but no matching song in database for bundle: ${output.bundle}" }
                }
            }

            inputPointer != null && outputPointer == null -> {
                log.info { "Detected SALE END: $input" }
                val found =
                    transaction {
                        MarketplaceSaleEntity.getByPointer(inputPointer)?.run {
                            status = SaleStatus.Ended
                        } != null
                    }
                if (!found) {
                    log.warn { "Detected SALE END, but no sale in database for pointer: $inputPointer" }
                }
            }

            inputPointer != null && input.bundleQuantity > 0 -> {
                val outputBundleQuantity = output?.bundleQuantity.orZero()
                val isSoldOut = outputBundleQuantity == 0L
                val purchaseBundleQuantity = input.bundleQuantity - outputBundleQuantity
                log.info { "Detected PURCHASE of $purchaseBundleQuantity bundles${if (isSoldOut) " (SOLD-OUT)" else ""}: $input" }
                val found =
                    transaction {
                        MarketplaceSaleEntity.getByPointer(inputPointer)?.run {
                            if (isSoldOut) status = SaleStatus.SoldOut
                            availableBundleQuantity = outputBundleQuantity
                            MarketplacePurchaseEntity.new {
                                createdAt = response.timestamp.epochSecondsToLocalDateTime()
                                saleId = this@run.id
                                bundleQuantity = purchaseBundleQuantity
                            }
                        } != null
                    }
                if (!found) {
                    log.warn { "Detected PURCHASE, but no sale in database for pointer: $inputPointer" }
                }
            }
        }
        transaction {
            MarketplaceBookmarkEntity.update(SALE_TIP_KEY, response)
        }
    }

    override suspend fun processQueueTransaction(response: MonitorAddressResponse) {
        log.info { "Processing queue transaction ${response.txId}, block ${response.block}, slot ${response.slot}" }

        val isMainnet = cardanoRepository.isMainnet()
        val input = response.spentUtxosList.firstOrNull()?.parseQueue(isMainnet)
        val output = response.createdUtxosList.firstOrNull()?.parseQueue(isMainnet)
        val sale =
            output?.let {
                transaction {
                    MarketplaceSaleEntity.getByPointer(pointerPolicyId, it.pointerAssetName)
                }
            }
        val payment = sale?.let { output.getToken(it.costPolicyId, it.costAssetName) }

        // TODO: More work needed here, for now we only print logs.
        when {
            output != null && (output.pointerAssetName.isBlank() || output.numberOfBundles <= 0) -> {
                log.warn { "Detected INVALID state: $output" }
            }

            payment != null && payment.amount >= sale.costAmount && sale.availableBundleQuantity >= sale.bundleAmount -> {
                log.info { "Detected PURCHASE + REFUND: $output" }
            }

            payment != null && ((payment.amount < sale.costAmount) || (sale.availableBundleQuantity == 0L)) -> {
                log.info { "Detected REFUND ONLY: $output" }
            }

            output == null && input != null -> {
                log.info { "Detected REFUND ONLY (because of sold-out sale): $input" }
            }
        }
        transaction {
            MarketplaceBookmarkEntity.update(QUEUE_TIP_KEY, response)
        }
    }

    private fun Token.getMatchingSongId(): EntityID<UUID>? =
        SongTable.select(SongTable.id).where {
            (SongTable.nftPolicyId eq policyId) and (SongTable.nftName eq assetName)
        }.limit(1).firstOrNull()?.get(SongTable.id)

    private suspend fun MarketplaceSaleEntity.getCostAmountUsd(): String {
        val unitPrice = cardanoRepository.queryNativeTokenUSDPrice(costPolicyId, costAssetName)
        return (costAmount.toBigInteger() * unitPrice.toBigInteger()).toAdaString()
    }
}
