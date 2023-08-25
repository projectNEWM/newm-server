package io.newm.server.features.collaboration.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.model.Collaborator
import io.newm.server.features.collaboration.model.CollaboratorFilters
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.user.database.UserEntity
import io.newm.server.ktx.asMandatoryField
import io.newm.server.ktx.asValidEmail
import io.newm.server.ktx.checkLength
import io.newm.shared.exception.HttpConflictException
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.millisToMinutesSecondsString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.util.UUID

internal class CollaborationRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val emailRepository: EmailRepository
) : CollaborationRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun add(collaboration: Collaboration, requesterId: UUID): UUID {
        logger.debug { "add: collaboration = $collaboration" }

        val songId = collaboration.songId.asMandatoryField("songId")
        val email = collaboration.email.asValidEmail()
        collaboration.checkFieldLengths()

        return transaction {
            checkSongState(songId, requesterId)
            checkUniqueEmail(songId, email)
            CollaborationEntity.new {
                this.songId = EntityID(songId, SongTable)
                this.email = email
                this.role = collaboration.role
                this.royaltyRate = collaboration.royaltyRate?.toFloat()
                collaboration.credited?.let { this.credited = it }
                collaboration.featured?.let { this.featured = it }
                collaboration.distributionArtistId?.let { this.distributionArtistId = it }
            }.id.value
        }
    }

    override suspend fun update(
        collaboration: Collaboration,
        collaborationId: UUID,
        requesterId: UUID,
        skipStatusCheck: Boolean
    ) {
        logger.debug { "update: collaboration = $collaboration, collaborationId = $collaborationId" }

        collaboration.checkFieldLengths()

        val entity = transaction {
            CollaborationEntity[collaborationId].apply {
                if (!skipStatusCheck) {
                    checkSongState(requesterId)
                    if (!userMatches(requesterId, email)) {
                        checkStatus(CollaborationStatus.Editing, CollaborationStatus.Rejected)
                    }
                }
                collaboration.email?.let { email = it.asValidUniqueEmail(this) }
                collaboration.role?.let { role = it }
                collaboration.royaltyRate?.let { royaltyRate = it.toFloat() }
                collaboration.credited?.let { credited = it }
                collaboration.featured?.let { featured = it }
                collaboration.distributionArtistId?.let { distributionArtistId = it }
            }
        }

        if (entity.status == CollaborationStatus.Rejected) {
            invite(entity.songId.value) { listOf(entity) }
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
        logger.debug { "getAll: userId = $userId, filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            CollaborationEntity.all(userId, filters)
                .limit(n = limit, offset = offset.toLong())
                .map(CollaborationEntity::toModel)
        }
    }

    override suspend fun getAllCount(userId: UUID, filters: CollaborationFilters): Long {
        logger.debug { "getAllCount: userId = $userId, filters = $filters" }
        return transaction {
            CollaborationEntity.all(userId, filters).count()
        }
    }

    override suspend fun getCollaborators(
        userId: UUID,
        filters: CollaboratorFilters,
        offset: Int,
        limit: Int
    ): List<Collaborator> {
        logger.debug { "getCollaborators: userId = $userId, filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            CollaborationEntity.collaborators(userId, filters)
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

    override suspend fun getAllBySongId(songId: UUID): List<Collaboration> = getAll(
        userId = transaction { SongEntity[songId].ownerId.value },
        filters = CollaborationFilters(songIds = listOf(songId)),
        offset = 0,
        limit = Integer.MAX_VALUE
    )

    override suspend fun getCollaboratorCount(userId: UUID, filters: CollaboratorFilters): Long {
        logger.debug { "getCollaboratorCount: userId = $userId, filters = $filters" }
        return transaction {
            CollaborationEntity.collaborators(userId, filters).count()
        }
    }

    override suspend fun reply(collaborationId: UUID, requesterId: UUID, accepted: Boolean) {
        logger.debug { "reply: collaborationId = $collaborationId, accepted = $accepted" }
        transaction {
            val collaboration = CollaborationEntity[collaborationId]
            if (!userMatches(requesterId, collaboration.email)) {
                throw HttpForbiddenException("Operation allowed only by collaborator")
            }
            collaboration.checkStatus(CollaborationStatus.Waiting)
            collaboration.status = if (accepted) CollaborationStatus.Accepted else CollaborationStatus.Rejected
        }
    }

    override suspend fun invite(songId: UUID) {
        logger.debug { "invite: songId = $songId" }
        invite(songId) { CollaborationEntity.findBySongId(songId) }
    }

    private suspend fun invite(songId: UUID, getCollaborations: Transaction.() -> Iterable<CollaborationEntity>) {
        val emails = mutableListOf<String>()
        val song = transaction {
            val song = SongEntity[songId]
            val owner = UserEntity[song.ownerId]
            getCollaborations().forEach { collab ->
                collab.status = if (collab.email.equals(owner.email, ignoreCase = true)) {
                    CollaborationStatus.Accepted
                } else {
                    emails += collab.email
                    CollaborationStatus.Waiting
                }
            }
            song
        }
        if (emails.isNotEmpty()) {
            emailRepository.send(
                to = emptyList(),
                bcc = emails,
                subject = environment.getConfigString("collaboration.email.subject"),
                messageUrl = environment.getConfigString("collaboration.email.messageUrl"),
                messageArgs = mapOf(
                    "title" to song.title,
                    "duration" to song.duration?.toLong()?.millisToMinutesSecondsString().orEmpty()
                )
            )
        }
    }

    private fun userMatches(userId: UUID, email: String?): Boolean =
        UserEntity[userId].email.equals(email, ignoreCase = true)

    private fun CollaborationEntity.checkSongState(requesterId: UUID, edit: Boolean = true) =
        checkSongState(songId.value, requesterId, email, edit)

    private fun checkSongState(songId: UUID, requesterId: UUID, email: String? = null, edit: Boolean = true) {
        with(SongEntity[songId]) {
            if (ownerId.value != requesterId) {
                if (edit) {
                    throw HttpForbiddenException("Operation allowed only by owner")
                } else if (!userMatches(requesterId, email)) {
                    throw HttpForbiddenException("Operation allowed only by owner or collaborator")
                }
            }
            if (edit && (mintingStatus.ordinal > MintingStatus.AwaitingCollaboratorApproval.ordinal)) {
                throw HttpUnprocessableEntityException("Operation not allowed when mintingStatus = $mintingStatus")
            }
        }
    }

    private fun Collaboration.checkFieldLengths() {
        role?.checkLength("role")
    }

    private fun CollaborationEntity.checkStatus(vararg statuses: CollaborationStatus = arrayOf(CollaborationStatus.Editing)) {
        if (status !in statuses) {
            throw HttpUnprocessableEntityException("Operation only allowed when status: ${statuses.joinToString()}")
        }
    }

    private fun String.asValidUniqueEmail(entity: CollaborationEntity): String {
        val email = asValidEmail()
        if (!email.equals(entity.email, ignoreCase = true)) {
            checkUniqueEmail(entity.songId.value, this)
        }
        return email
    }

    private fun checkUniqueEmail(songId: UUID, email: String) {
        if (CollaborationEntity.exists(songId, email)) {
            throw HttpConflictException("Collaboration already exists: songId = $songId, email = $email")
        }
    }
}
