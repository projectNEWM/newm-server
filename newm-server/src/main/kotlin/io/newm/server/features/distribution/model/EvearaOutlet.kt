package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvearaOutlet(
    @SerialName("process_duration_dates")
    val processDurationDates: Long,
    @SerialName("dolby_enabled")
    val dolbyEnabled: Boolean,
    @SerialName("store_name")
    val storeName: String,
    @SerialName("is_presales_date_enabled")
    val isPresalesDateEnabled: Boolean,
    @SerialName("sony360_enabled")
    val sony360Enabled: Boolean,
    @SerialName("store_id")
    val storeId: Long,
    @SerialName("child_stores")
    val childStores: List<String>
)
