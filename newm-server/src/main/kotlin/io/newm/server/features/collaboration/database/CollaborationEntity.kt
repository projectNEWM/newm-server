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
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
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

    fun toModel(): Collaboration = Collaboration(
        id = id.value,
        createdAt = createdAt,
        songId = songId.value,
        email = email,
        role = role,
        royaltyRate = royaltyRate,
        credited = credited,
        featured = featured,
        status = status
    )

    companion object : UUIDEntityClass<CollaborationEntity>(CollaborationTable) {
        fun exists(songId: UUID, email: String): Boolean = exists {
            (CollaborationTable.songId eq songId) and (CollaborationTable.email.lowerCase() eq email.lowercase())
        }

        fun all(userId: UUID, filters: CollaborationFilters): SizedIterable<CollaborationEntity> {
            val inbound = filters.inbound == true
            val ops = mutableListOf<Op<Boolean>>()
            ops += if (inbound) {
                CollaborationTable.email.lowerCase() eq UserEntity[userId].email.lowercase()
            } else {
                SongTable.ownerId eq userId
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
                statuses?.let {
                    ops += CollaborationTable.status inList it
                }
            }
            val andOp = AndOp(ops)
            return if (inbound) {
                find(andOp)
            } else {
                CollaborationEntity.wrapRows(
                    CollaborationTable.join(
                        otherTable = SongTable,
                        joinType = JoinType.INNER,
                        additionalConstraint = { CollaborationTable.songId eq SongTable.id }
                    ).slice(CollaborationTable.columns).select(andOp)
                )
            }
        }

        fun findBySongId(songId: UUID): SizedIterable<CollaborationEntity> =
            find { CollaborationTable.songId eq songId }

        fun collaborators(userId: UUID, filters: CollaboratorFilters): SizedIterable<Pair<String, Long>> {
            val ops = mutableListOf<Op<Boolean>>()
            ops += SongTable.ownerId eq userId
            with(filters) {
                songIds?.let {
                    ops += CollaborationTable.songId inList it
                }
                phrase?.let {
                    val pattern = "%${it.lowercase()}%"
                    ops += (
                        (CollaborationTable.email.lowerCase() like pattern)
                            or (UserTable.firstName.lowerCase() like pattern)
                            or (UserTable.lastName.lowerCase() like pattern)
                        )
                }
                return CollaborationTable.join(
                    otherTable = SongTable,
                    joinType = JoinType.INNER,
                    additionalConstraint = { CollaborationTable.songId eq SongTable.id }
                ).join(
                    otherTable = UserTable,
                    joinType = JoinType.LEFT,
                    additionalConstraint = { CollaborationTable.email.lowerCase() eq UserTable.email.lowerCase() }
                ).slice(CollaborationTable.email.lowerCase(), SongTable.id.count())
                    .select(AndOp(ops))
                    .groupBy(CollaborationTable.email.lowerCase())
                    .mapLazy { it[CollaborationTable.email.lowerCase()] to it[SongTable.id.count()] }
            }
        }
    }
}
