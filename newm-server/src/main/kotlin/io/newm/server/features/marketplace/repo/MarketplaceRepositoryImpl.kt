package io.newm.server.features.marketplace.repo

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toHexString
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_INCENTIVE_MIN_AMOUNT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PENDING_ORDER_TTL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_LOVELACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NFTCDN_ENABLED
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.marketplace.builders.buildQueueDatum
import io.newm.server.features.marketplace.builders.buildSaleDatum
import io.newm.server.features.marketplace.database.MarketplaceArtistEntity
import io.newm.server.features.marketplace.database.MarketplaceBookmarkEntity
import io.newm.server.features.marketplace.database.MarketplacePendingOrderEntity
import io.newm.server.features.marketplace.database.MarketplacePendingSaleEntity
import io.newm.server.features.marketplace.database.MarketplacePurchaseEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.marketplace.model.OrderAmountRequest
import io.newm.server.features.marketplace.model.OrderAmountResponse
import io.newm.server.features.marketplace.model.OrderTransactionRequest
import io.newm.server.features.marketplace.model.OrderTransactionResponse
import io.newm.server.features.marketplace.model.QueueTransaction
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleAmountRequest
import io.newm.server.features.marketplace.model.SaleAmountResponse
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.SaleTransactionRequest
import io.newm.server.features.marketplace.model.SaleTransactionResponse
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.marketplace.parser.parseQueue
import io.newm.server.features.marketplace.parser.parseSale
import io.newm.server.features.song.database.SongTable
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpPaymentRequiredException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.epochSecondsToLocalDateTime
import io.newm.shared.ktx.orZero
import io.newm.txbuilder.ktx.mergeAmounts
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private const val SALE_TIP_KEY = "saleTip"
private const val QUEUE_TIP_KEY = "queueTip"

internal class MarketplaceRepositoryImpl(
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository
) : MarketplaceRepository {
    private val log = KotlinLogging.logger {}

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
        val sales = transaction {
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
            MarketplaceArtistEntity
                .all(filters)
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

    override suspend fun generateSaleAmount(request: SaleAmountRequest): SaleAmountResponse {
        log.debug { "generateSaleAmount: $request" }

        val nativeAssets = listOf(
            nativeAsset {
                policy = request.bundlePolicyId
                name = request.bundleAssetName
                amount = computeAmount(request.bundleAmount, request.totalBundleQuantity)
            }
        )

        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_SALE_LOVELACE)
        val amount = CborArray.create(
            listOf(
                CborInteger.create(lovelace.toBigInteger()),
                nativeAssets.toNativeAssetCborMap()
            )
        )

        val pendingSaleTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL)
        val costPolicyId = request.costPolicyId ?: configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID)
        val costAssetName =
            request.costAssetName ?: configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME)
        val sale = transaction {
            MarketplacePendingSaleEntity.deleteAllExpired(pendingSaleTimeToLive)
            MarketplacePendingSaleEntity.new {
                this.ownerAddress = request.ownerAddress
                this.bundlePolicyId = request.bundlePolicyId
                this.bundleAssetName = request.bundleAssetName
                this.bundleAmount = request.bundleAmount
                this.costPolicyId = costPolicyId
                this.costAssetName = costAssetName
                this.costAmount = request.costAmount
                this.totalBundleQuantity = request.totalBundleQuantity
            }
        }

        return SaleAmountResponse(
            saleId = sale.id.value,
            amountCborHex = amount.toCborByteArray().toHexString()
        )
    }

    override suspend fun generateSaleTransaction(request: SaleTransactionRequest): SaleTransactionResponse {
        log.debug { "generateSaleTransaction: $request" }

        if (request.utxos.isEmpty()) {
            throw HttpPaymentRequiredException("Missing UTXO's")
        }

        val pendingSaleTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL)
        val sale = transaction {
            MarketplacePendingSaleEntity.deleteAllExpired(pendingSaleTimeToLive)
            MarketplacePendingSaleEntity[request.saleId]
        }

        val contractAddress = configRepository.getString(CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS)
        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_SALE_LOVELACE)
        val transaction = cardanoRepository.buildTransaction {
            sourceUtxos.addAll(request.utxos)
            outputUtxos.add(
                outputUtxo {
                    address = contractAddress
                    this.lovelace = lovelace.toString()
                    nativeAssets.addAll(
                        listOf(
                            nativeAsset {
                                policy = sale.bundlePolicyId
                                name = sale.bundleAssetName
                                amount = computeAmount(sale.bundleAmount, sale.totalBundleQuantity)
                            }
                        )
                    )
                    datum = buildSaleDatum(
                        ownerAddress = sale.ownerAddress,
                        bundleToken = Token(
                            policyId = sale.bundlePolicyId,
                            assetName = sale.bundleAssetName,
                            amount = sale.bundleAmount
                        ),
                        costToken = Token(
                            policyId = sale.costPolicyId,
                            assetName = sale.costAssetName,
                            amount = sale.costAmount
                        ),
                        maxBundleSize = sale.totalBundleQuantity // TODO: revisit this value
                    ).toCborObject().toCborByteArray().toHexString()
                }
            )
            changeAddress = request.changeAddress
        }
        if (transaction.hasErrorMessage()) {
            throw HttpUnprocessableEntityException("Failed to build sale transaction: ${transaction.errorMessage}")
        }
        transaction {
            sale.delete()
        }
        return SaleTransactionResponse(transaction.transactionCbor.toByteArray().toHexString())
    }

    override suspend fun generateOrderAmount(request: OrderAmountRequest): OrderAmountResponse {
        log.debug { "generateOrderAmount: $request" }

        val sale = transaction { MarketplaceSaleEntity[request.saleId] }
        require(request.bundleQuantity <= sale.availableBundleQuantity) {
            "Bundle quantity (${request.bundleQuantity}) exceeds sale availability (${sale.availableBundleQuantity})"
        }

        val incentiveMinAmount = configRepository.getLong(CONFIG_KEY_MARKETPLACE_INCENTIVE_MIN_AMOUNT)
        val incentiveAmount = request.incentiveAmount?.also {
            require(it >= incentiveMinAmount) {
                "Incentive amount ($it) is less than minimum required ($incentiveMinAmount)"
            }
        } ?: incentiveMinAmount

        val incentivePolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID)
        val incentiveAssetName = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME)
        val nativeAssets = listOf(
            nativeAsset {
                policy = sale.costPolicyId
                name = sale.costAssetName
                amount = computeAmount(sale.costAmount, request.bundleQuantity)
            },
            nativeAsset {
                policy = incentivePolicyId
                name = incentiveAssetName
                amount = computeAmount(incentiveAmount, 2)
            }
        )

        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE)
        val amount = CborArray.create(
            listOf(
                CborInteger.create(lovelace.toBigInteger()),
                nativeAssets.toNativeAssetCborMap()
            )
        )

        val pendingOrderTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_ORDER_TTL)
        val order = transaction {
            MarketplacePendingOrderEntity.deleteAllExpired(pendingOrderTimeToLive)
            MarketplacePendingOrderEntity.new {
                this.saleId = sale.id
                this.bundleQuantity = request.bundleQuantity
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

        val pendingOrderTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_ORDER_TTL)
        val (order, sale) = transaction {
            MarketplacePendingOrderEntity.deleteAllExpired(pendingOrderTimeToLive)
            MarketplacePendingOrderEntity[request.orderId].let { it to MarketplaceSaleEntity[it.saleId] }
        }

        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE)

        val contractAddress = configRepository.getString(CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS)
        val incentivePolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID)
        val incentiveAssetName = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME)
        val transaction = cardanoRepository.buildTransaction {
            sourceUtxos.addAll(request.utxos)
            outputUtxos.add(
                outputUtxo {
                    address = contractAddress
                    this.lovelace = lovelace.toString()
                    nativeAssets.addAll(
                        listOf(
                            nativeAsset {
                                policy = sale.costPolicyId
                                name = sale.costAssetName
                                amount = computeAmount(sale.costAmount, order.bundleQuantity)
                            },
                            nativeAsset {
                                policy = incentivePolicyId
                                name = incentiveAssetName
                                amount = computeAmount(order.incentiveAmount, 2)
                            }
                        ).mergeAmounts()
                    )
                    datum = buildQueueDatum(
                        ownerAddress = sale.ownerAddress,
                        numberOfBundles = order.bundleQuantity,
                        incentiveToken = Token(
                            policyId = incentivePolicyId,
                            assetName = incentiveAssetName,
                            amount = order.incentiveAmount
                        ),
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
        return OrderTransactionResponse(transaction.transactionCbor.toByteArray().toHexString())
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
        val pointerPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID)
        val inputPointer = input?.getToken(pointerPolicyId)
        val outputPointer = output?.getToken(pointerPolicyId)

        when {
            input == null && output != null && outputPointer == null -> {
                log.info { "Detected NEW SALE (not started yet): $output" }
            }

            inputPointer == null && outputPointer != null -> {
                log.info { "Detected SALE START: $input" }
                val found = transaction {
                    val songId = output.bundle.getMatchingSongId() ?: return@transaction false
                    MarketplaceSaleEntity.new {
                        this.createdAt = response.timestamp.epochSecondsToLocalDateTime()
                        this.status = SaleStatus.Started
                        this.songId = songId
                        this.ownerAddress = output.ownerAddress
                        this.pointerPolicyId = outputPointer.policyId
                        this.pointerAssetName = outputPointer.assetName
                        this.bundlePolicyId = output.bundle.policyId
                        this.bundleAssetName = output.bundle.assetName
                        this.bundleAmount = output.bundle.amount
                        this.costPolicyId = output.cost.policyId
                        this.costAssetName = output.cost.assetName
                        this.costAmount = output.cost.amount
                        this.maxBundleSize = output.maxBundleSize
                        this.totalBundleQuantity = output.bundleQuantity
                        this.availableBundleQuantity = output.bundleQuantity
                    }
                    true
                }
                if (!found) {
                    log.warn { "Detected SALE START, but no matching song in database for bundle: ${output.bundle}" }
                }
            }

            inputPointer != null && outputPointer == null -> {
                log.info { "Detected SALE END: $input" }
                val found = transaction {
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
                val found = transaction {
                    MarketplaceSaleEntity.getByPointer(inputPointer)?.run {
                        if (isSoldOut) status = SaleStatus.SoldOut
                        availableBundleQuantity = outputBundleQuantity
                        MarketplacePurchaseEntity.new {
                            this.createdAt = response.timestamp.epochSecondsToLocalDateTime()
                            this.saleId = this@run.id
                            this.bundleQuantity = purchaseBundleQuantity
                        }
                    } != null
                }
                if (!found) {
                    log.warn { "Detected PURCHASE, but no sale in database for pointer: $inputPointer" }
                }
            }
        }
        updateSaleTip(response)
    }

    override suspend fun processQueueTransaction(response: MonitorAddressResponse) {
        log.info { "Processing queue transaction ${response.txId}, block ${response.block}, slot ${response.slot}" }

        val isMainnet = cardanoRepository.isMainnet()
        val input: QueueTransaction?
        val output: QueueTransaction?
        try {
            input = response.spentUtxosList.firstOrNull()?.parseQueue(isMainnet)
            output = response.createdUtxosList.firstOrNull()?.parseQueue(isMainnet)
        } catch (e: Throwable) {
            log.error(e) { "Failed to parse queue transaction: ${response.txId}" }
            updateQueueTip(response)
            return
        }

        val pointerPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID)
        val sale = output?.let {
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
        updateQueueTip(response)
    }

    private fun updateSaleTip(response: MonitorAddressResponse) {
        transaction {
            MarketplaceBookmarkEntity.update(SALE_TIP_KEY, response)
        }
    }

    private fun updateQueueTip(response: MonitorAddressResponse) {
        transaction {
            MarketplaceBookmarkEntity.update(QUEUE_TIP_KEY, response)
        }
    }

    private fun computeAmount(
        multiplier: Long,
        multiplicand: Long
    ) = (multiplier.toBigInteger() * multiplicand.toBigInteger()).toString()

    private fun Token.getMatchingSongId(): EntityID<UUID>? =
        SongTable
            .select(SongTable.id)
            .where {
                (SongTable.nftPolicyId eq policyId) and (SongTable.nftName eq assetName)
            }.limit(1)
            .firstOrNull()
            ?.get(SongTable.id)

    private suspend fun MarketplaceSaleEntity.getCostAmountUsd(): String {
        val unitPrice = cardanoRepository.queryNativeTokenUSDPrice(costPolicyId, costAssetName)
        return (costAmount.toBigInteger() * unitPrice.toBigInteger()).toBigDecimal(12).toPlainString()
    }
}
