package io.newm.server.statuspages

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val code: Int,
    val description: String,
    val cause: String
)
