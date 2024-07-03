package io.newm.server.features.marketplace.repo

import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.marketplace.model.OrderAmountRequest
import io.newm.server.features.marketplace.model.OrderAmountResponse
import io.newm.server.features.marketplace.model.OrderTransactionRequest
import io.newm.server.features.marketplace.model.OrderTransactionResponse
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

    suspend fun generateSaleStartAmount(request: SaleStartAmountRequest): SaleStartAmountResponse

    suspend fun generateSaleStartTransaction(request: SaleStartTransactionRequest): SaleStartTransactionResponse

    suspend fun generateSaleEndAmount(request: SaleEndAmountRequest): SaleEndAmountResponse

    suspend fun generateSaleEndTransaction(request: SaleEndTransactionRequest): SaleEndTransactionResponse

    suspend fun generateOrderAmount(request: OrderAmountRequest): OrderAmountResponse

    suspend fun generateOrderTransaction(request: OrderTransactionRequest): OrderTransactionResponse

    suspend fun getSaleTransactionTip(): String?

    suspend fun getQueueTransactionTip(): String?

    suspend fun processSaleTransaction(response: MonitorAddressResponse)

    suspend fun processQueueTransaction(response: MonitorAddressResponse)
}
