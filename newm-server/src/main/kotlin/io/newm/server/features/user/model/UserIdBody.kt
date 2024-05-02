package io.newm.server.features.user.model

import io.newm.server.typealiases.UserId
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UserIdBody(
    @Serializable(with = UUIDSerializer::class)
    val userId: UserId
)
