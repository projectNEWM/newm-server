package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutletProfile(
    @SerialName("id")
    val id: Long,
    @SerialName("profile_url")
    val profileUrl: String,
    /**
     * Only used for AudioMack
     */
    @SerialName("profile_id")
    val profileId: String? = null,
)
