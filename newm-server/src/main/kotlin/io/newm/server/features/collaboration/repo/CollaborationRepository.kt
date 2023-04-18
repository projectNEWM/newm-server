package io.newm.server.features.collaboration.repo

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.Collaborator
import java.util.UUID

interface CollaborationRepository {
    suspend fun add(collaboration: Collaboration, requesterId: UUID): UUID
    suspend fun update(collaboration: Collaboration, collaborationId: UUID, requesterId: UUID)
    suspend fun delete(collaborationId: UUID, requesterId: UUID)
    suspend fun get(collaborationId: UUID, requesterId: UUID): Collaboration
    suspend fun getAll(userId: UUID, filters: CollaborationFilters, offset: Int, limit: Int): List<Collaboration>
    suspend fun getAllCount(userId: UUID, filters: CollaborationFilters): Long
    suspend fun getCollaborators(userId: UUID, offset: Int, limit: Int): List<Collaborator>
    suspend fun getCollaboratorCount(userId: UUID): Long
}
