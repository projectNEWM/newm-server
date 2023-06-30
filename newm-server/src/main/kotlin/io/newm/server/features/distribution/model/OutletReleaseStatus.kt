package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutletReleaseStatus(
    @SerialName("store_id")
    val storeId: Long,
    @SerialName("store_name")
    val storeName: String,
    @SerialName("outlet_status")
    val outletStatus: OutletStatus,
    @SerialName("pre_sales_date")
    val preSalesDate: String,
    @SerialName("release_start_date")
    val releaseStartDate: String,
    @SerialName("release_end_date")
    val releaseEndDate: String,
    @SerialName("process_duration_dates")
    val processDurationDates: Int,
    @SerialName("is_presales_date_enabled")
    val isPresalesDateEnabled: Boolean,
    @SerialName("child_stores")
    val childStores: List<String>? = null,
    @SerialName("price_code_list")
    val priceCodeList: PriceCodeList? = null,
    @SerialName("price_code")
    val priceCode: PriceCode? = null,
)
