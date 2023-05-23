package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Outlet(
    @SerialName("outlet_name")
    val outletName: String? = null,
    @SerialName("outlet_id")
    val outletId: String? = null,
)
