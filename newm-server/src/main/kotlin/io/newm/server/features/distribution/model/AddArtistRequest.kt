package io.newm.server.features.distribution.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AddArtistRequest(
    @SerialName("uuid")
    @Serializable(with = UUIDSerializer::class)
    val evearaUserUuid: UUID,
    @SerialName("name")
    val name: String,
    @SerialName("country")
    val country: String,
    @SerialName("outlets_profile")
    val outletsProfile: OutletsProfile? = null
)
