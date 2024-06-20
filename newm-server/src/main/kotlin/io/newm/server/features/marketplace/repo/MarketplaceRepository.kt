package io.newm.server.features.marketplace.repo

import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.marketplace.model.OrderAmountRequest
import io.newm.server.features.marketplace.model.OrderAmountResponse
import io.newm.server.features.marketplace.model.OrderTransactionRequest
import io.newm.server.features.marketplace.model.OrderTransactionResponse
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleAmountRequest
import io.newm.server.features.marketplace.model.SaleAmountResponse
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleTransactionRequest
import io.newm.server.features.marketplace.model.SaleTransactionResponse
import io.newm.server.typealiases.UserId
import java.util.UUID

interface MarketplaceRepository {
    suspend fun getSale(saleId: UUID): Sale

    suspend fun getSales(
        filters: SaleFilters,
        offset: Int,
        limit: Int
    ): List<Sale>

    suspend fun getSaleCount(filters: SaleFilters): Long

    suspend fun getArtist(artistId: UserId): Artist

    suspend fun getArtists(
        filters: ArtistFilters,
        offset: Int,
        limit: Int
    ): List<Artist>

    suspend fun getArtistCount(filters: ArtistFilters): Long

    suspend fun generateSaleAmount(request: SaleAmountRequest): SaleAmountResponse

    suspend fun generateSaleTransaction(request: SaleTransactionRequest): SaleTransactionResponse

    suspend fun generateOrderAmount(request: OrderAmountRequest): OrderAmountResponse

    suspend fun generateOrderTransaction(request: OrderTransactionRequest): OrderTransactionResponse

    suspend fun getSaleTransactionTip(): String?

    suspend fun getQueueTransactionTip(): String?

    suspend fun processSaleTransaction(response: MonitorAddressResponse)

    suspend fun processQueueTransaction(response: MonitorAddressResponse)
}
