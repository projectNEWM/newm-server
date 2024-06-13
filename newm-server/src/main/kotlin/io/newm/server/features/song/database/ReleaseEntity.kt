package io.newm.server.features.song.database

import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.ReleaseBarcodeType
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.ReleaseId
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.exists
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import java.time.LocalDate
import java.time.LocalDateTime

class ReleaseEntity(id: EntityID<ReleaseId>) : UUIDEntity(id) {
    var archived: Boolean by ReleaseTable.archived
    val createdAt: LocalDateTime by ReleaseTable.createdAt
    var ownerId: EntityID<UserId> by ReleaseTable.ownerId
    var title: String by ReleaseTable.title
    var releaseType: ReleaseType by ReleaseTable.releaseType
    var distributionReleaseId: Long? by ReleaseTable.distributionReleaseId
    var barcodeType: ReleaseBarcodeType? by ReleaseTable.barcodeType
    var barcodeNumber: String? by ReleaseTable.barcodeNumber
    var releaseDate: LocalDate? by ReleaseTable.releaseDate
    var publicationDate: LocalDate? by ReleaseTable.publicationDate
    var coverArtUrl: String? by ReleaseTable.coverArtUrl
    var arweaveCoverArtUrl: String? by ReleaseTable.arweaveCoverArtUrl
    var hasSubmittedForDistribution: Boolean by ReleaseTable.hasSubmittedForDistribution
    var errorMessage: String? by ReleaseTable.errorMessage
    var forceDistributed: Boolean? by ReleaseTable.forceDistributed
    var preSavePage: String? by ReleaseTable.preSavePage

    fun toModel(): Release =
        Release(
            id = id.value,
            archived = archived,
            ownerId = ownerId.value,
            createdAt = createdAt,
            title = title,
            releaseType = releaseType,
            distributionReleaseId = distributionReleaseId,
            coverArtUrl = coverArtUrl,
            arweaveCoverArtUrl = arweaveCoverArtUrl,
            barcodeType = barcodeType,
            barcodeNumber = barcodeNumber,
            releaseDate = releaseDate,
            publicationDate = publicationDate,
            hasSubmittedForDistribution = hasSubmittedForDistribution,
            errorMessage = errorMessage,
            forceDistributed = forceDistributed,
            preSavePage = preSavePage
        )

    companion object : UUIDEntityClass<ReleaseEntity>(ReleaseTable) {
        fun all(filters: SongFilters): SizedIterable<ReleaseEntity> {
            val ops = filters.toOps()
            return when {
                ops.isEmpty() -> all()
                filters.phrase == null -> find(AndOp(ops))
                else ->
                    ReleaseEntity.wrapRows(
                        ReleaseTable.innerJoin(
                            otherTable = UserTable,
                            onColumn = { ownerId },
                            otherColumn = { id }
                        ).selectAll().where(AndOp(ops))
                    )
            }.orderBy(ReleaseTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        fun exists(
            ownerId: UserId,
            title: String
        ): Boolean =
            exists {
                (ReleaseTable.archived eq false) and
                    (ReleaseTable.ownerId eq ownerId) and
                    (ReleaseTable.title.lowerCase() eq title.lowercase())
            }

        private fun SongFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
            ops += ReleaseTable.archived eq (archived ?: false)
            olderThan?.let {
                ops += ReleaseTable.createdAt less it
            }
            newerThan?.let {
                ops += ReleaseTable.createdAt greater it
            }
            ids?.includes?.let {
                ops += ReleaseTable.id inList it
            }
            ids?.excludes?.let {
                ops += ReleaseTable.id notInList it
            }
            ownerIds?.includes?.let {
                ops += ReleaseTable.ownerId inList it
            }
            ownerIds?.excludes?.let {
                ops += ReleaseTable.ownerId notInList it
            }
            phrase?.let {
                val pattern = "%${it.lowercase()}%"
                ops += (
                    (ReleaseTable.title.lowerCase() like pattern)
                        or (UserTable.nickname.lowerCase() like pattern)
                        or (
                            (UserTable.nickname eq null) and (
                                (UserTable.firstName.lowerCase() like pattern)
                                    or (UserTable.lastName.lowerCase() like pattern)
                            )
                        )
                )
            }
            return ops
        }
    }
}
