package io.newm.server.features.distribution.model

import io.newm.shared.serialization.DMYLocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class OutletsDetail(
    @SerialName("store_id")
    val storeId: Long,
    @SerialName("pre_sales_date")
    @Serializable(with = DMYLocalDateSerializer::class)
    val preSalesDate: LocalDate? = null,
    @SerialName("release_start_date")
    @Serializable(with = DMYLocalDateSerializer::class)
    val releaseStartDate: LocalDate,
    @SerialName("release_end_date")
    @Serializable(with = DMYLocalDateSerializer::class)
    val releaseEndDate: LocalDate? = null,
    @SerialName("price_code")
    val priceCode: PriceCode? = null,
)
