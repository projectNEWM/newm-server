package io.newm.server.features.marketplace.repo

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.protobuf.kotlin.toByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.util.Sha3
import io.newm.chain.util.extractCredentials
import io.newm.chain.util.extractStakeKeyHex
import io.newm.chain.util.hexStringToAssetName
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import io.newm.chain.util.toRequiredSigner
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_INCENTIVE_MIN_AMOUNT
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PENDING_ORDER_TTL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_POINTER_ASSET_NAME_PREFIX
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_PROFIT_AMOUNT_USD
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_LOVELACE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_REFERENCE_INPUT_UTXOS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SERVICE_FEE_PERCENTAGE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_USD_POLICY_ID
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_USD_PRICE_ADJUSTMENT_PERCENTAGE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NFTCDN_ENABLED
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.marketplace.builders.buildOrderTransaction
import io.newm.server.features.marketplace.builders.buildQueueDatum
import io.newm.server.features.marketplace.builders.buildSaleDatum
import io.newm.server.features.marketplace.builders.buildSaleEndTransaction
import io.newm.server.features.marketplace.builders.buildSaleStartTransaction
import io.newm.server.features.marketplace.database.MarketplaceArtistEntity
import io.newm.server.features.marketplace.database.MarketplaceBookmarkEntity
import io.newm.server.features.marketplace.database.MarketplacePendingOrderEntity
import io.newm.server.features.marketplace.database.MarketplacePendingSaleEntity
import io.newm.server.features.marketplace.database.MarketplacePurchaseEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleOwnerEntity
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.marketplace.model.CostAmountConversions
import io.newm.server.features.marketplace.model.OrderAmountRequest
import io.newm.server.features.marketplace.model.OrderAmountResponse
import io.newm.server.features.marketplace.model.OrderFees
import io.newm.server.features.marketplace.model.OrderTransactionRequest
import io.newm.server.features.marketplace.model.OrderTransactionResponse
import io.newm.server.features.marketplace.model.QueueTransaction
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleEndAmountRequest
import io.newm.server.features.marketplace.model.SaleEndAmountResponse
import io.newm.server.features.marketplace.model.SaleEndTransactionRequest
import io.newm.server.features.marketplace.model.SaleEndTransactionResponse
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStartAmountRequest
import io.newm.server.features.marketplace.model.SaleStartAmountResponse
import io.newm.server.features.marketplace.model.SaleStartTransactionRequest
import io.newm.server.features.marketplace.model.SaleStartTransactionResponse
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.SaleTransaction
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.marketplace.parser.parseQueue
import io.newm.server.features.marketplace.parser.parseSale
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.toReferenceUtxo
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpNotFoundException
import io.newm.shared.exception.HttpPaymentRequiredException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.epochSecondsToLocalDateTime
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.toGroupingString
import io.newm.txbuilder.ktx.extractFields
import io.newm.txbuilder.ktx.sortByHashAndIx
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import java.math.BigInteger
import java.util.UUID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

private const val SALE_TIP_KEY = "saleTip"
private const val QUEUE_TIP_KEY = "queueTip"

private const val CASH_REGISTER_KEY_NAME = "cashRegister"
private const val COLLATERAL_KEY_NAME = "collateral"

private const val SALE_STARTED_EVENT = "saleStarted"
private const val SALE_ENDED_EVENT = "saleEnded"
private const val SALE_SOLD_OUT_EVENT = "saleSoldOut"

internal class MarketplaceRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository,
    private val emailRepository: EmailRepository,
    private val songRepository: SongRepository
) : MarketplaceRepository {
    private val log = KotlinLogging.logger {}

    override suspend fun getSale(saleId: UUID): Sale {
        log.debug { "getSale: saleId = $saleId" }
        val sale = transaction { MarketplaceSaleEntity[saleId] }
        val isMainnet = cardanoRepository.isMainnet()
        val isNftCdnEnabled = configRepository.getBoolean(CONFIG_KEY_NFTCDN_ENABLED)
        val costAmountConversions = sale.computeCostAmountConversions()
        val smartLinks = sale.getSmartLinks()
        return transaction { sale.toModel(isMainnet, isNftCdnEnabled, costAmountConversions, smartLinks) }
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
            MarketplaceSaleEntity
                .all(filters)
                .offset(start = offset.toLong())
                .limit(count = limit)
                .toList()
        }
        val conversions = sales.associate { it.id.value to it.computeCostAmountConversions() }
        val smartLinks = sales.associate { it.id.value to it.getSmartLinks() }
        return transaction {
            sales.map { it.toModel(isMainnet, isNftCdnEnabled, conversions[it.id.value]!!, smartLinks[it.id.value]!!) }
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
                .offset(start = offset.toLong())
                .limit(count = limit)
                .map(MarketplaceArtistEntity::toModel)
        }
    }

    override suspend fun getArtistCount(filters: ArtistFilters): Long {
        log.debug { "getArtistCount: filters = $filters" }
        return transaction {
            MarketplaceArtistEntity.all(filters).count()
        }
    }

    override suspend fun generateSaleStartAmount(request: SaleStartAmountRequest): SaleStartAmountResponse {
        log.debug { "generateSaleStartAmount: $request" }

        val nativeAssets = listOf(
            nativeAsset {
                policy = request.bundlePolicyId
                name = request.bundleAssetName
                amount = computeAmount(request.bundleAmount, request.totalBundleQuantity)
            }
        )

        // add 2 extra ADA lovelace to ensure we always return change back and avoid a potential minUtxoNotMet error
        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_SALE_LOVELACE) + 2000000L
        val amount = CborArray.create(
            listOf(
                CborInteger.create(lovelace.toBigInteger()),
                nativeAssets.toNativeAssetCborMap()
            )
        )

        val usdPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_USD_POLICY_ID)
        val costPolicyId: String
        val costAssetName: String
        if (request.costPolicyId == null && request.costAssetName == null) {
            costPolicyId = usdPolicyId
            costAssetName = ""
        } else {
            costPolicyId = requireNotNull(request.costPolicyId) { "costPolicyId must not be null" }
            costAssetName = requireNotNull(request.costAssetName) { "costAssetName must not be null" }
            if (costPolicyId == usdPolicyId) require(costAssetName.isEmpty()) { "costAssetName must be empty for USD" }
        }

        val pendingSaleTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL)
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

        return SaleStartAmountResponse(
            saleId = sale.id.value,
            amountCborHex = amount.toCborByteArray().toHexString()
        )
    }

    override suspend fun generateSaleStartTransaction(request: SaleStartTransactionRequest): SaleStartTransactionResponse {
        log.debug { "generateSaleStartTransaction: $request" }

        if (request.utxos.isEmpty()) {
            throw HttpPaymentRequiredException("Missing UTXO's")
        }

        val pendingSaleTimeToLive = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PENDING_SALE_TTL)
        val sale = transaction {
            MarketplacePendingSaleEntity.deleteAllExpired(pendingSaleTimeToLive)
            MarketplacePendingSaleEntity[request.saleId]
        }

        val bundleAsset = nativeAsset {
            policy = sale.bundlePolicyId
            name = sale.bundleAssetName
            amount = computeAmount(sale.bundleAmount, sale.totalBundleQuantity)
        }

        val pointerAsset = nativeAsset {
            policy = configRepository.getString(CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID)
            name = computePointerAssetName(request.utxos)
            amount = "1"
        }

        val saleDatum = buildSaleDatum(
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
            maxBundleSize = Long.MAX_VALUE // auto-computed in the contract
        )

        val referenceInputUtxos = configRepository
            .getStrings(CONFIG_KEY_MARKETPLACE_SALE_REFERENCE_INPUT_UTXOS)
            .map(String::toReferenceUtxo)

        val contractAddress = configRepository.getString(CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS)
        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_SALE_LOVELACE)

        val cashRegisterKey = getKey(CASH_REGISTER_KEY_NAME)
        val collateralKey = getKey(COLLATERAL_KEY_NAME)
        val signingKeys = listOf(cashRegisterKey, collateralKey)
        val dummyKeys = request.utxos
            .map { it.address.extractCredentials().first }
            .distinct()
            .map { Key.generateNew() }
        val allKeys = signingKeys + dummyKeys
        val requiredSigners = signingKeys.map { it.requiredSigner().toByteString() }
        val collateralUtxos = getCollateralUtxos(collateralKey.address)

        // calculate required fee, totalCollateral and redeemer
        val calculatedFields = cardanoRepository
            .buildSaleStartTransaction(
                sourceUtxos = request.utxos,
                collateralUtxos = collateralUtxos,
                referenceInputUtxos = referenceInputUtxos,
                contractAddress = contractAddress,
                changeAddress = request.changeAddress,
                lovelace = lovelace,
                bundleAsset = bundleAsset,
                pointerAsset = pointerAsset,
                saleDatum = saleDatum,
                requiredSigners = requiredSigners,
                signatures = cardanoRepository.signTransactionDummy(allKeys)
            ).extractFields()

        // generate transaction id
        val transactionId = cardanoRepository
            .buildSaleStartTransaction(
                sourceUtxos = request.utxos,
                collateralUtxos = collateralUtxos,
                referenceInputUtxos = referenceInputUtxos,
                contractAddress = contractAddress,
                changeAddress = request.changeAddress,
                lovelace = lovelace,
                bundleAsset = bundleAsset,
                pointerAsset = pointerAsset,
                saleDatum = saleDatum,
                requiredSigners = requiredSigners,
                signatures = cardanoRepository.signTransactionDummy(allKeys),
                fee = calculatedFields.fee,
                totalCollateral = calculatedFields.totalCollateral,
                redeemers = calculatedFields.redeemers
            ).transactionId

        // generate signed transaction
        val transaction = cardanoRepository.buildSaleStartTransaction(
            sourceUtxos = request.utxos,
            collateralUtxos = collateralUtxos,
            referenceInputUtxos = referenceInputUtxos,
            contractAddress = contractAddress,
            changeAddress = request.changeAddress,
            lovelace = lovelace,
            bundleAsset = bundleAsset,
            pointerAsset = pointerAsset,
            saleDatum = saleDatum,
            requiredSigners = requiredSigners,
            signatures = cardanoRepository.signTransaction(transactionId.hexToByteArray(), signingKeys),
            fee = calculatedFields.fee,
            totalCollateral = calculatedFields.totalCollateral,
            redeemers = calculatedFields.redeemers
        )
        transaction {
            sale.delete()
            request.email?.let {
                MarketplaceSaleOwnerEntity.new {
                    pointerPolicyId = pointerAsset.policy
                    pointerAssetName = pointerAsset.name
                    email = it
                }
            }
        }
        return SaleStartTransactionResponse(transaction.transactionCbor.toByteArray().toHexString())
    }

    override suspend fun generateSaleEndAmount(request: SaleEndAmountRequest): SaleEndAmountResponse {
        log.debug { "generateSaleEndAmount: $request" }

        val exists = transaction { MarketplaceSaleEntity.existsHavingId(request.saleId) }
        if (!exists) {
            throw HttpNotFoundException("Sale not found: ${request.saleId}")
        }

        // add 2 extra ADA lovelace to ensure we always return change back and avoid a potential minUtxoNotMet error
        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_SALE_LOVELACE) + 2000000L
        val amount = CborInteger.create(lovelace.toBigInteger())
        return SaleEndAmountResponse(amount.toCborByteArray().toHexString())
    }

    override suspend fun generateSaleEndTransaction(request: SaleEndTransactionRequest): SaleEndTransactionResponse {
        log.debug { "generateSaleEndTransaction: $request" }

        if (request.utxos.isEmpty()) {
            throw HttpPaymentRequiredException("Missing UTXO's")
        }

        val sale = transaction {
            MarketplaceSaleEntity[request.saleId]
        }

        // find the UTXO in the contract that holds the pointer asset, bundle tokens left and any profit
        val contractAddress = configRepository.getString(CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS)
        val saleUtxo = cardanoRepository.queryLiveUtxos(contractAddress).firstOrNull { utxo ->
            utxo.nativeAssetsList.any { it.policy == sale.pointerPolicyId && it.name == sale.pointerAssetName }
        } ?: throw HttpUnprocessableEntityException("Pointer asset not found in contract address utxos")

        val sourceUtxos = (request.utxos + saleUtxo).sortByHashAndIx()
        val saleUtxoIndex = sourceUtxos.indexOf(saleUtxo).toLong()

        val pointerAsset = nativeAsset {
            policy = sale.pointerPolicyId
            name = sale.pointerAssetName
            amount = "-1"
        }

        val referenceInputUtxos = configRepository
            .getStrings(CONFIG_KEY_MARKETPLACE_SALE_REFERENCE_INPUT_UTXOS)
            .map(String::toReferenceUtxo)

        val cashRegisterKey = getKey(CASH_REGISTER_KEY_NAME)
        val collateralKey = getKey(COLLATERAL_KEY_NAME)
        val signingKeys = listOf(cashRegisterKey, collateralKey)
        val dummyKeys = request.utxos
            .map { it.address }
            .plus(sale.ownerAddress)
            .map { it.extractCredentials().first }
            .distinct()
            .map { Key.generateNew() }
        val allKeys = signingKeys + dummyKeys
        val requiredSigners = signingKeys.map { it.requiredSigner().toByteString() } +
            listOf(sale.ownerAddress.toRequiredSigner().toByteString())
        val collateralUtxos = getCollateralUtxos(collateralKey.address)

        // calculate required fee, totalCollateral and redeemer
        val calculatedFields = cardanoRepository
            .buildSaleEndTransaction(
                saleUtxoIndex = saleUtxoIndex,
                sourceUtxos = sourceUtxos,
                collateralUtxos = collateralUtxos,
                referenceInputUtxos = referenceInputUtxos,
                ownerAddress = sale.ownerAddress,
                changeAddress = request.changeAddress,
                pointerAsset = pointerAsset,
                requiredSigners = requiredSigners,
                signatures = cardanoRepository.signTransactionDummy(allKeys)
            ).extractFields()

        // generate transaction id
        val transactionId = cardanoRepository
            .buildSaleEndTransaction(
                saleUtxoIndex = saleUtxoIndex,
                sourceUtxos = sourceUtxos,
                collateralUtxos = collateralUtxos,
                referenceInputUtxos = referenceInputUtxos,
                ownerAddress = sale.ownerAddress,
                changeAddress = request.changeAddress,
                pointerAsset = pointerAsset,
                requiredSigners = requiredSigners,
                signatures = cardanoRepository.signTransactionDummy(allKeys),
                fee = calculatedFields.fee,
                totalCollateral = calculatedFields.totalCollateral,
                redeemers = calculatedFields.redeemers
            ).transactionId

        // generate signed transaction
        val transaction = cardanoRepository.buildSaleEndTransaction(
            saleUtxoIndex = saleUtxoIndex,
            sourceUtxos = sourceUtxos,
            collateralUtxos = collateralUtxos,
            referenceInputUtxos = referenceInputUtxos,
            ownerAddress = sale.ownerAddress,
            changeAddress = request.changeAddress,
            pointerAsset = pointerAsset,
            requiredSigners = requiredSigners,
            signatures = cardanoRepository.signTransaction(transactionId.hexToByteArray(), signingKeys),
            fee = calculatedFields.fee,
            totalCollateral = calculatedFields.totalCollateral,
            redeemers = calculatedFields.redeemers
        )
        return SaleEndTransactionResponse(transaction.transactionCbor.toByteArray().toHexString())
    }

    override suspend fun getOrderFees(): OrderFees {
        log.debug { "getOrderFees" }

        return OrderFees(
            profitAmountUsd = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PROFIT_AMOUNT_USD) * 1E-12,
            serviceFeePercentage = configRepository.getDouble(CONFIG_KEY_MARKETPLACE_SERVICE_FEE_PERCENTAGE)
        )
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

        // 2 incentive units required: 1 for purchase and 1 for removal
        val totalIncentiveAmount = incentiveAmount.toBigInteger() * BigInteger.TWO

        val currencyPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID)
        val currencyAssetName = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME)
        val currencyUsdPrice =
            cardanoRepository.queryNativeTokenUSDPrice(currencyPolicyId, currencyAssetName).toBigInteger()

        val usdPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_USD_POLICY_ID)
        val profitAmountUsd = configRepository.getLong(CONFIG_KEY_MARKETPLACE_PROFIT_AMOUNT_USD).toBigInteger()

        val usdPriceAdjustmentFraction =
            configRepository.getDouble(CONFIG_KEY_MARKETPLACE_USD_PRICE_ADJUSTMENT_PERCENTAGE) / 100.0
        val serviceFeeFraction = configRepository.getDouble(CONFIG_KEY_MARKETPLACE_SERVICE_FEE_PERCENTAGE) / 100.0

        val nativeAssets = mutableListOf<NativeAsset>()
        val orderCostAmount = sale.costAmount.toBigInteger() * request.bundleQuantity.toBigInteger()
        val serviceFeeAmount = (orderCostAmount.toBigDecimal() * serviceFeeFraction.toBigDecimal()).toBigInteger()
        val subtotalAmountUsd = when (sale.costPolicyId) {
            usdPolicyId -> orderCostAmount + serviceFeeAmount + profitAmountUsd
            else -> {
                nativeAssets += nativeAsset {
                    policy = sale.costPolicyId
                    name = sale.costAssetName
                    amount = (orderCostAmount + serviceFeeAmount).toString()
                }
                profitAmountUsd
            }
        }
        val extraAmountUsd =
            (subtotalAmountUsd.toBigDecimal() * usdPriceAdjustmentFraction.toBigDecimal()).toBigInteger()
        val currencyAmount = ((subtotalAmountUsd + extraAmountUsd) / currencyUsdPrice + totalIncentiveAmount).toString()
        nativeAssets += nativeAsset {
            policy = currencyPolicyId
            name = currencyAssetName
            amount = currencyAmount
        }

        // add 3 extra ADA lovelace to ensure we always return change back and avoid a potential minUtxoNotMet error
        val lovelace = configRepository.getLong(CONFIG_KEY_MARKETPLACE_ORDER_LOVELACE) + 3000000L
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
                this.serviceFeeAmount = when (sale.costPolicyId) {
                    usdPolicyId -> serviceFeeAmount / currencyUsdPrice
                    else -> serviceFeeAmount
                }.toString()
                this.currencyAmount = currencyAmount
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
        val currencyPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_POLICY_ID)
        val currencyAssetName = configRepository.getString(CONFIG_KEY_MARKETPLACE_CURRENCY_ASSET_NAME)
        val usdPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_USD_POLICY_ID)

        val contractNativeAssets = mutableListOf<NativeAsset>()
        val contractCurrencyAmount = when (sale.costPolicyId) {
            usdPolicyId -> (order.currencyAmount.toBigInteger() - order.serviceFeeAmount.toBigInteger()).toString()
            else -> {
                contractNativeAssets += nativeAsset {
                    policy = sale.costPolicyId
                    name = sale.costAssetName
                    amount = computeAmount(sale.costAmount, order.bundleQuantity)
                }
                order.currencyAmount
            }
        }
        contractNativeAssets += nativeAsset {
            policy = currencyPolicyId
            name = currencyAssetName
            amount = contractCurrencyAmount
        }
        val cashRegisterNativeAssets = listOf(
            nativeAsset {
                policy = currencyPolicyId
                name = currencyAssetName
                amount = order.serviceFeeAmount
            }
        )

        val transaction = cardanoRepository.buildOrderTransaction(
            sourceUtxos = request.utxos,
            contractAddress = contractAddress,
            changeAddress = request.changeAddress,
            cashRegisterAddress = getKey(CASH_REGISTER_KEY_NAME).address,
            lovelace = lovelace,
            contractNativeAssets = contractNativeAssets,
            cashRegisterNativeAssets = cashRegisterNativeAssets,
            queueDatum = buildQueueDatum(
                ownerAddress = request.changeAddress,
                numberOfBundles = order.bundleQuantity,
                incentiveToken = Token(
                    policyId = currencyPolicyId,
                    assetName = currencyAssetName,
                    amount = order.incentiveAmount
                ),
                pointerAssetName = sale.pointerAssetName
            )
        )
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
        val input: SaleTransaction?
        val output: SaleTransaction?
        try {
            input = response.spentUtxosList.firstOrNull()?.parseSale(isMainnet)
            output = response.createdUtxosList.firstOrNull()?.parseSale(isMainnet)
        } catch (e: Throwable) {
            log.error(e) { "Failed to parse sale transaction: ${response.txId}" }
            updateSaleTip(response)
            return
        }

        val pointerPolicyId = configRepository.getString(CONFIG_KEY_MARKETPLACE_POINTER_POLICY_ID)
        val inputPointer = input?.getToken(pointerPolicyId)
        val outputPointer = output?.getToken(pointerPolicyId)

        when {
            input == null && output != null && outputPointer == null -> {
                log.info { "Detected NEW SALE (not started yet): $output" }
            }

            inputPointer == null && outputPointer != null -> {
                log.info { "Detected SALE START: $output" }
                val sale = transaction {
                    output.bundle
                        .getMatchingSongId()
                        ?.let {
                            MarketplaceSaleEntity.new {
                                this.createdAt = response.timestamp.epochSecondsToLocalDateTime()
                                this.status = SaleStatus.Started
                                this.songId = it
                                this.ownerAddress = output.ownerAddress
                                this.ownerAddressStakeKey = output.ownerAddress.extractStakeKeyHex()
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
                        }
                }
                if (sale == null) {
                    log.warn { "Detected SALE START, but no matching song in database for bundle: ${output.bundle}" }
                } else {
                    notifySaleEvent(SALE_STARTED_EVENT, sale)
                }
            }

            inputPointer != null && outputPointer == null -> {
                log.info { "Detected SALE END: $input" }
                val sale = transaction {
                    MarketplaceSaleEntity.getByPointer(inputPointer)?.apply {
                        status = SaleStatus.Ended
                    }
                }
                if (sale == null) {
                    log.warn { "Detected SALE END, but no sale in database for pointer: $inputPointer" }
                } else {
                    notifySaleEvent(SALE_ENDED_EVENT, sale)
                }
            }

            inputPointer != null && input.bundleQuantity > 0 -> {
                val outputBundleQuantity = output?.bundleQuantity.orZero()
                val isSoldOut = outputBundleQuantity == 0L
                val purchaseBundleQuantity = input.bundleQuantity - outputBundleQuantity
                log.info { "Detected PURCHASE of $purchaseBundleQuantity bundles${if (isSoldOut) " (SOLD-OUT)" else ""}: $input" }
                val sale = transaction {
                    MarketplaceSaleEntity.getByPointer(inputPointer)?.apply {
                        if (isSoldOut) status = SaleStatus.SoldOut
                        availableBundleQuantity = outputBundleQuantity
                        MarketplacePurchaseEntity.new {
                            this.createdAt = response.timestamp.epochSecondsToLocalDateTime()
                            this.saleId = this@apply.id
                            this.bundleQuantity = purchaseBundleQuantity
                        }
                    }
                }
                if (sale == null) {
                    log.warn { "Detected PURCHASE, but no sale in database for pointer: $inputPointer" }
                } else if (isSoldOut) {
                    notifySaleEvent(SALE_SOLD_OUT_EVENT, sale)
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

    private suspend fun getKey(name: String): Key =
        requireNotNull(
            cardanoRepository.getKeyByName(name)
        ) { "$name key not defined!" }

    private suspend fun getCollateralUtxos(address: String): List<Utxo> =
        listOf(
            requireNotNull(
                cardanoRepository
                    .queryLiveUtxos(address)
                    .filter { it.nativeAssetsCount == 0 }
                    .maxByOrNull { it.lovelace.toLong() }
            ) { "collateral utxo not found!" }
        )

    private fun computeAmount(
        multiplier: Long,
        multiplicand: Long
    ) = (multiplier.toBigInteger() * multiplicand.toBigInteger()).toString()

    private suspend fun computePointerAssetName(utxos: List<Utxo>): String {
        val prefix = configRepository.getString(CONFIG_KEY_MARKETPLACE_POINTER_ASSET_NAME_PREFIX)
        val refUtxo = utxos.sortByHashAndIx().first()
        val txHash = Sha3.hash256(refUtxo.hash.hexToByteArray())
        return prefix + (byteArrayOf(refUtxo.ix.toByte()) + txHash.copyOfRange(0, 31 - prefix.length / 2)).toHexString()
    }

    private fun Token.getMatchingSongId(): EntityID<UUID>? =
        SongTable
            .select(SongTable.id)
            .where {
                (SongTable.nftPolicyId eq policyId) and (SongTable.nftName eq assetName)
            }.limit(1)
            .firstOrNull()
            ?.get(SongTable.id)

    private suspend fun MarketplaceSaleEntity.computeCostAmountConversions(): CostAmountConversions {
        val newmUsdPrice = cardanoRepository.queryNEWMUSDPrice()
        val costTokenUsdPrice = when {
            // for USD, 1 rather that 1000000 to account for costAmount being in 12 instead of 6 decimal places
            costPolicyId == configRepository.getString(CONFIG_KEY_MARKETPLACE_USD_POLICY_ID) -> 1L

            cardanoRepository.isNewmToken(costPolicyId, costAssetName) -> newmUsdPrice

            else -> cardanoRepository.queryNativeTokenUSDPrice(costPolicyId, costAssetName)
        }
        val costAmountUsd = costAmount.toBigInteger() * costTokenUsdPrice.toBigInteger()
        return CostAmountConversions(
            usd = costAmountUsd.toBigDecimal(12).toPlainString(),
            newm = (costAmountUsd.toBigDecimal(6) / newmUsdPrice.toBigDecimal()).toPlainString()
        )
    }

    private suspend fun MarketplaceSaleEntity.computeTotalValue(): String {
        val value = totalBundleQuantity.toBigInteger() * costAmount.toBigInteger()
        return when {
            costPolicyId == configRepository.getString(CONFIG_KEY_MARKETPLACE_USD_POLICY_ID) -> {
                "${value.toBigDecimal(12).toGroupingString()} USD"
            }

            cardanoRepository.isNewmToken(costPolicyId, costAssetName) -> {
                "${value.toBigDecimal(6).toGroupingString()} &#413;"
            }

            else -> {
                "${value.toBigDecimal(6).toGroupingString()} ${costAssetName.hexStringToAssetName()}"
            }
        }
    }

    private suspend fun notifySaleEvent(
        event: String,
        sale: MarketplaceSaleEntity
    ) {
        var title: String? = null
        val email = transaction {
            val owner = MarketplaceSaleOwnerEntity.getByPointer(sale.pointerPolicyId, sale.pointerAssetName)
            owner?.email?.also {
                title = SongEntity[sale.songId].title
                if (event == SALE_SOLD_OUT_EVENT || event == SALE_ENDED_EVENT) {
                    owner.delete()
                }
            }
        } ?: return

        val messageArgs = mutableMapOf("song" to title.orEmpty())
        if (event == SALE_STARTED_EVENT || event == SALE_SOLD_OUT_EVENT) {
            messageArgs += "quantity" to sale.totalBundleQuantity.toGroupingString()
            messageArgs += "value" to sale.computeTotalValue()
        }
        emailRepository.send(
            to = email,
            subject = environment.getConfigString("marketplace.email.$event.subject"),
            messageUrl = environment.getConfigString("marketplace.email.$event.messageUrl"),
            messageArgs = messageArgs
        )
    }

    private suspend fun MarketplaceSaleEntity.getSmartLinks(): List<SongSmartLink> =
        try {
            songRepository.getSmartLinks(songId.value)
        } catch (e: Throwable) {
            // in case of Eveara service failure, log and return an empty list rather than failing the whole operation
            log.warn(e) { "Failed to get smart-links for song: ${songId.value}" }
            emptyList()
        }
}
