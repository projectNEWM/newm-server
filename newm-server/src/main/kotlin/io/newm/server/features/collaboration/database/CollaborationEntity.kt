package io.newm.server.features.collaboration.database

import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.model.CollaboratorFilters
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.shared.ktx.exists
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.util.UUID

class CollaborationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt: LocalDateTime by CollaborationTable.createdAt
    var songId: EntityID<UUID> by CollaborationTable.songId
    var email: String by CollaborationTable.email
    var role: String? by CollaborationTable.role
    var royaltyRate: Float? by CollaborationTable.royaltyRate
    var credited: Boolean by CollaborationTable.credited
    var featured: Boolean by CollaborationTable.featured
    var status: CollaborationStatus by CollaborationTable.status
    var distributionArtistId: Long? by CollaborationTable.distributionArtistId
    var distributionParticipantId: Long? by CollaborationTable.distributionParticipantId

    fun toModel(): Collaboration =
        Collaboration(
            id = id.value,
            createdAt = createdAt,
            songId = songId.value,
            email = email,
            role = role,
            royaltyRate = royaltyRate?.toBigDecimal(),
            credited = credited,
            featured = featured,
            status = status,
            distributionArtistId = distributionArtistId,
            distributionParticipantId = distributionParticipantId
        )

    companion object : UUIDEntityClass<CollaborationEntity>(CollaborationTable) {
        fun exists(
            songId: UUID,
            email: String
        ): Boolean =
            exists {
                (CollaborationTable.songId eq songId) and (CollaborationTable.email.lowerCase() eq email.lowercase())
            }

        fun exists(
            ownerId: UUID,
            email: String,
            status: CollaborationStatus
        ): Boolean {
            return CollaborationTable.innerJoin(
                otherTable = SongTable,
                onColumn = { songId },
                otherColumn = { id }
            ).selectAll().where {
                (SongTable.archived eq false) and
                    (SongTable.ownerId eq ownerId) and
                    (CollaborationTable.email.lowerCase() eq email.lowercase()) and
                    (CollaborationTable.status eq status)
            }.any()
        }

        fun all(
            userId: UUID,
            filters: CollaborationFilters
        ): SizedIterable<CollaborationEntity> {
            val inbound = filters.inbound == true
            val ops = mutableListOf<Op<Boolean>>()
            if (inbound) {
                ops += CollaborationTable.email.lowerCase() eq UserEntity[userId].email.lowercase()
            } else {
                ops += SongTable.archived eq false
                ops += SongTable.ownerId eq userId
            }
            with(filters) {
                olderThan?.let {
                    ops += CollaborationTable.createdAt less it
                }
                newerThan?.let {
                    ops += CollaborationTable.createdAt greater it
                }
                ids?.let {
                    ops += CollaborationTable.id inList it
                }
                songIds?.let {
                    ops += CollaborationTable.songId inList it
                }
                emails?.let {
                    ops += CollaborationTable.email.lowerCase() inList it.map(String::lowercase)
                }
                statuses?.let {
                    ops += CollaborationTable.status inList it
                }
            }
            val andOp = AndOp(ops)
            val res =
                if (inbound) {
                    find(andOp)
                } else {
                    CollaborationEntity.wrapRows(
                        CollaborationTable.innerJoin(
                            otherTable = SongTable,
                            onColumn = { songId },
                            otherColumn = { id }
                        ).selectAll().where(andOp)
                    )
                }
            return res.orderBy(CollaborationTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        fun findBySongId(songId: UUID): SizedIterable<CollaborationEntity> = find { CollaborationTable.songId eq songId }

        fun collaborators(
            userId: UUID,
            filters: CollaboratorFilters
        ): SizedIterable<Pair<String, Long>> {
            val ops = mutableListOf<Op<Boolean>>()
            ops += SongTable.archived eq false
            ops += SongTable.ownerId eq userId
            with(filters) {
                if (excludeMe == true) {
                    ops += CollaborationTable.email.lowerCase() neq UserEntity[userId].email.lowercase()
                }
                songIds?.let {
                    ops += CollaborationTable.songId inList it
                }
                emails?.let {
                    ops += CollaborationTable.email.lowerCase() inList it.map(String::lowercase)
                }
                phrase?.let {
                    val pattern = "%${it.lowercase()}%"
                    ops += (CollaborationTable.email.lowerCase() like pattern) or
                        (UserTable.firstName.lowerCase() like pattern) or
                        (UserTable.lastName.lowerCase() like pattern)
                }
                return CollaborationTable.innerJoin(
                    otherTable = SongTable,
                    onColumn = { songId },
                    otherColumn = { id }
                ).leftJoin(
                    otherTable = UserTable,
                    onColumn = { CollaborationTable.email.lowerCase() },
                    otherColumn = { email.lowerCase() }
                ).select(CollaborationTable.email.lowerCase(), SongTable.id.count())
                    .where(AndOp(ops))
                    .groupBy(CollaborationTable.email.lowerCase())
                    .orderBy(CollaborationTable.email.lowerCase(), filters.sortOrder ?: SortOrder.ASC)
                    .mapLazy { it[CollaborationTable.email.lowerCase()] to it[SongTable.id.count()] }
            }
        }
    }
}
