package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Preview(
    @SerialName("start_at")
    val startAt: Long,
    @SerialName("duration")
    val duration: Long
)
