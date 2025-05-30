package io.newm.server.features.collaboration.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Collaboration(
    @Contextual
    val id: UUID? = null,
    @Contextual
    val createdAt: LocalDateTime? = null,
    @Contextual
    val songId: SongId? = null,
    val email: String? = null,
    @Deprecated("Use 'roles' instead")
    val role: String? = null, // TODO: STUD-460 - remove "role" field after frontend migrates to use "roles"
    val roles: List<String>? = null,
    @Contextual
    val royaltyRate: BigDecimal? = null,
    val credited: Boolean? = null,
    val featured: Boolean? = null,
    val status: CollaborationStatus? = null,
    val distributionArtistId: Long? = null,
    val distributionParticipantId: Long? = null,
)
