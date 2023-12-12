package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserLabelData(
    @SerialName("releases")
    val releases: Int? = null,
    @SerialName("label_id")
    val labelId: Long? = null,
    @SerialName("is_active")
    val isActive: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("removable")
    val removable: Boolean? = null
)
