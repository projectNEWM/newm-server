package io.newm.server.features.collaboration.repo

import io.ktor.util.logging.Logger
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.model.Collaborator
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.user.database.UserEntity
import io.newm.server.ktx.asMandatoryField
import io.newm.server.ktx.asValidEmail
import io.newm.server.ktx.checkLength
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.util.UUID

internal class CollaborationRepositoryImpl : CollaborationRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun add(collaboration: Collaboration, requesterId: UUID): UUID {
        logger.debug { "add: collaboration = $collaboration" }

        val songId = collaboration.songId.asMandatoryField("songId")
        val email = collaboration.email.asValidEmail()
        collaboration.checkFieldLengths()

        return transaction {
            checkSongState(songId, requesterId)
            CollaborationEntity.new {
                this.songId = EntityID(songId, SongTable)
                this.email = email
                this.role = collaboration.role
                this.royaltyRate = collaboration.royaltyRate
                collaboration.credited?.let { this.credited = it }
            }.id.value
        }
    }

    override suspend fun update(collaboration: Collaboration, collaborationId: UUID, requesterId: UUID) {
        logger.debug { "update: collaboration = $collaboration, collaborationId = $collaborationId" }

        collaboration.checkFieldLengths()

        transaction {
            val entity = CollaborationEntity[collaborationId]
            entity.checkSongState(requesterId)
            entity.checkStatus()
            collaboration.email?.let { entity.email = it.asValidEmail() }
            collaboration.role?.let { entity.role = it }
            collaboration.royaltyRate?.let { entity.royaltyRate = it }
            collaboration.credited?.let { entity.credited = it }
        }
    }

    override suspend fun delete(collaborationId: UUID, requesterId: UUID) {
        logger.debug { "delete: collaborationId = $collaborationId" }
        transaction {
            val entity = CollaborationEntity[collaborationId]
            entity.checkSongState(requesterId)
            entity.checkStatus()
            entity.delete()
        }
    }

    override suspend fun get(collaborationId: UUID, requesterId: UUID): Collaboration {
        logger.debug { "get: collaborationId = $collaborationId" }
        return transaction {
            val entity = CollaborationEntity[collaborationId]
            entity.checkSongState(requesterId, false)
            entity.toModel()
        }
    }

    override suspend fun getAll(
        userId: UUID,
        filters: CollaborationFilters,
        offset: Int,
        limit: Int
    ): List<Collaboration> {
        logger.debug { "getAll: userId  $userId, filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            CollaborationEntity.all(userId, filters)
                .limit(n = limit, offset = offset.toLong())
                .map(CollaborationEntity::toModel)
        }
    }

    override suspend fun getAllCount(userId: UUID, filters: CollaborationFilters): Long {
        logger.debug { "getAllCount: userId  $userId, filters = $filters" }
        return transaction {
            CollaborationEntity.all(userId, filters).count()
        }
    }

    override suspend fun getCollaborators(userId: UUID, offset: Int, limit: Int): List<Collaborator> {
        logger.debug { "getCollaborators: userId  $userId, offset = $offset, limit = $limit" }
        return transaction {
            CollaborationEntity.collaborators(userId)
                .limit(n = limit, offset = offset.toLong())
                .map { (email, songCount) ->
                    Collaborator(
                        email = email,
                        songCount = songCount,
                        user = UserEntity.getByEmail(email)?.toModel(false)
                    )
                }
        }
    }

    override suspend fun getCollaboratorCount(userId: UUID): Long {
        logger.debug { "getCollaboratorCount: userId  $userId" }
        return transaction {
            CollaborationEntity.collaborators(userId).count()
        }
    }

    override suspend fun reply(collaborationId: UUID, requesterId: UUID, accepted: Boolean) {
        logger.debug { "reply: collaborationId = $collaborationId, accepted = $accepted" }
        transaction {
            val collaboration = CollaborationEntity[collaborationId]
            val user = UserEntity[requesterId]
            if (!collaboration.email.equals(user.email, ignoreCase = true)) {
                throw HttpForbiddenException("Operation allowed only by collaborator")
            }
            collaboration.checkStatus(CollaborationStatus.Waiting)
            collaboration.status = if (accepted) CollaborationStatus.Accepted else CollaborationStatus.Rejected
        }
    }

    private fun CollaborationEntity.checkSongState(requesterId: UUID, edit: Boolean = true) =
        checkSongState(songId.value, requesterId, email, edit)

    private fun checkSongState(songId: UUID, requesterId: UUID, email: String? = null, edit: Boolean = true) {
        with(SongEntity[songId]) {
            if (ownerId.value != requesterId) {
                if (edit) {
                    throw HttpForbiddenException("Operation allowed only by owner")
                } else if (!UserEntity[requesterId].email.equals(email, ignoreCase = true)) {
                    throw HttpForbiddenException("Operation allowed only by owner or collaborator")
                }
            }
            if (edit && mintingStatus != MintingStatus.Undistributed) {
                throw HttpUnprocessableEntityException("Operation only allowed when mintingStatus = Undistributed")
            }
        }
    }

    private fun Collaboration.checkFieldLengths() {
        role?.checkLength("role")
    }

    private fun CollaborationEntity.checkStatus(status: CollaborationStatus = CollaborationStatus.Editing) {
        if (this.status != status) {
            throw HttpUnprocessableEntityException("Operation only allowed when status = $status")
        }
    }
}
