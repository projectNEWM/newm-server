package io.newm.server.features.marketplace.repo

import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.marketplace.database.MarketplaceBookmarkEntity
import io.newm.server.features.marketplace.database.MarketplacePurchaseEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.marketplace.parser.parseQueue
import io.newm.server.features.marketplace.parser.parseSale
import io.newm.server.features.song.database.SongTable
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.epochSecondsToLocalDateTime
import io.newm.shared.ktx.info
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.warn
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.UUID

private const val SALE_TIP_KEY = "saleTip"
private const val QUEUE_TIP_KEY = "queueTip"

internal class MarketplaceRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val cardanoRepository: CardanoRepository
) : MarketplaceRepository {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }

    private val pointerPolicyId: String by lazy {
        runBlocking {
            environment.getSecureConfigString("marketplace.pointerPolicyId")
        }
    }

    override suspend fun getSale(saleId: UUID): Sale {
        log.debug { "get: saleId = $saleId" }
        return transaction {
            MarketplaceSaleEntity[saleId].toModel()
        }
    }

    override suspend fun getSales(
        filters: SaleFilters,
        offset: Int,
        limit: Int
    ): List<Sale> {
        log.info { "getSales: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            MarketplaceSaleEntity.all(filters).limit(n = limit, offset = offset.toLong()).map(MarketplaceSaleEntity::toModel)
        }
    }

    override suspend fun getSaleCount(filters: SaleFilters): Long {
        log.debug { "getSaleCount: filters = $filters" }
        return transaction {
            MarketplaceSaleEntity.all(filters).count()
        }
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
}
