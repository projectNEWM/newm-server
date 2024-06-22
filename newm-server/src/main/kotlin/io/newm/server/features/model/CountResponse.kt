package io.newm.server.features.model

import kotlinx.serialization.Serializable

@Serializable
data class CountResponse(
    val count: Long
)
