package io.newm.server.features.marketplace.repo

import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleFilters
import java.util.UUID

interface MarketplaceRepository {
    suspend fun getSale(saleId: UUID): Sale

    suspend fun getSales(
        filters: SaleFilters,
        offset: Int,
        limit: Int
    ): List<Sale>

    suspend fun getSaleCount(filters: SaleFilters): Long

    suspend fun getSaleTransactionTip(): String?

    suspend fun getQueueTransactionTip(): String?

    suspend fun processSaleTransaction(response: MonitorAddressResponse)

    suspend fun processQueueTransaction(response: MonitorAddressResponse)
}
