package io.newm.server.features.dripdropz.model

import kotlinx.serialization.Serializable

@Serializable
data class CheckRequest(
    val address: String
)
