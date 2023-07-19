package io.newm.server.features.collaboration.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Collaboration(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = UUIDSerializer::class)
    val songId: UUID? = null,
    val email: String? = null,
    var role: String? = null,
    @Contextual
    val royaltyRate: BigDecimal? = null,
    val credited: Boolean? = null,
    val featured: Boolean? = null,
    val status: CollaborationStatus? = null,
    val distributionArtistId: Long? = null,
)
