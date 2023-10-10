package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddArtistRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("name")
    val name: String,
    @SerialName("country")
    val country: String,
    @SerialName("outlets_profile")
    val outletProfiles: List<OutletProfile>? = null
)
