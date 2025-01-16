package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartLink(
    @SerialName("store_name")
    val storeName: String,
    @SerialName("smart_link_url")
    val url: String
)
