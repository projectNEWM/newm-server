package io.newm.server.features.collaboration.repo

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.Collaborator
import io.newm.server.features.collaboration.model.CollaboratorFilters
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import java.util.UUID

interface CollaborationRepository {
    suspend fun add(
        collaboration: Collaboration,
        requesterId: UserId
    ): UUID

    suspend fun update(
        collaboration: Collaboration,
        collaborationId: UUID,
        requesterId: UserId,
        skipStatusCheck: Boolean = false
    )

    suspend fun delete(
        collaborationId: UUID,
        requesterId: UserId
    )

    suspend fun get(
        collaborationId: UUID,
        requesterId: UserId
    ): Collaboration

    suspend fun getAll(
        userId: UUID,
        filters: CollaborationFilters,
        offset: Int,
        limit: Int
    ): List<Collaboration>

    suspend fun getAllBySongId(songId: SongId): List<Collaboration>

    suspend fun getAllCount(
        userId: UUID,
        filters: CollaborationFilters
    ): Long

    suspend fun getCollaborators(
        userId: UUID,
        filters: CollaboratorFilters,
        offset: Int,
        limit: Int
    ): List<Collaborator>

    suspend fun getCollaboratorCount(
        userId: SongId,
        filters: CollaboratorFilters
    ): Long

    suspend fun reply(
        collaborationId: UUID,
        requesterId: UserId,
        accepted: Boolean
    ): Collaboration

    suspend fun invite(songId: SongId)
}
