package io.newm.server.features.collaboration.model

import io.newm.server.features.user.model.User
import kotlinx.serialization.Serializable

@Serializable
data class Collaborator(
    val email: String? = null,
    val songCount: Long? = null,
    val user: User? = null
)
