package io.newm.server.features.collaboration.repo

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.Collaborator
import io.newm.server.features.collaboration.model.CollaboratorFilters
import java.util.UUID

interface CollaborationRepository {
    suspend fun add(collaboration: Collaboration, requesterId: UUID): UUID
    suspend fun update(collaboration: Collaboration, collaborationId: UUID, requesterId: UUID, skipStatusCheck: Boolean = false)
    suspend fun delete(collaborationId: UUID, requesterId: UUID)
    suspend fun get(collaborationId: UUID, requesterId: UUID): Collaboration
    suspend fun getAll(userId: UUID, filters: CollaborationFilters, offset: Int, limit: Int): List<Collaboration>
    suspend fun getAllBySongId(songId: UUID): List<Collaboration>
    suspend fun getAllCount(userId: UUID, filters: CollaborationFilters): Long
    suspend fun getCollaborators(userId: UUID, filters: CollaboratorFilters, offset: Int, limit: Int): List<Collaborator>
    suspend fun getCollaboratorCount(userId: UUID, filters: CollaboratorFilters): Long
    suspend fun reply(collaborationId: UUID, requesterId: UUID, accepted: Boolean): Collaboration
    suspend fun invite(songId: UUID)
}
