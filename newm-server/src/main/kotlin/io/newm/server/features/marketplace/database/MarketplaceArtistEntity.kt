package io.newm.server.features.marketplace.database

import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.ArtistFilters
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.mapLazy

class MarketplaceArtistEntity(id: EntityID<UserId>) : UserEntity(id) {
    val releasedSongCount: Long
        get() =
            SongTable.select(SongTable.id)
                .where {
                    (SongTable.ownerId eq this@MarketplaceArtistEntity.id) and
                        (SongTable.archived eq false) and
                        (SongTable.mintingStatus eq MintingStatus.Released)
                }
                .count()

    val marketplaceSongCount: Long
        get() =
            SongTable.innerJoin(
                otherTable = MarketplaceSaleTable,
                onColumn = { id },
                otherColumn = { songId }
            ).select(SongTable.id)
                .where { SongTable.ownerId eq this@MarketplaceArtistEntity.id }
                .groupBy(SongTable.id)
                .count()

    fun toModel(): Artist =
        Artist(
            id = id.value,
            createdAt = createdAt,
            name = stageOrFullName,
            genre = genre,
            location = location,
            biography = biography,
            pictureUrl = pictureUrl,
            websiteUrl = websiteUrl,
            twitterUrl = twitterUrl,
            instagramUrl = instagramUrl,
            spotifyProfile = spotifyProfile,
            soundCloudProfile = soundCloudProfile,
            appleMusicProfile = appleMusicProfile,
            releasedSongCount = releasedSongCount,
            marketplaceSongCount = marketplaceSongCount
        )

    companion object : UUIDEntityClass<MarketplaceArtistEntity>(UserTable) {
        fun all(filters: ArtistFilters): SizedIterable<MarketplaceArtistEntity> {
            val ops = filters.toOps()
            return UserTable.innerJoin(
                otherTable = SongTable,
                onColumn = { id },
                otherColumn = { ownerId }
            ).select(UserTable.columns)
                .where(AndOp(ops))
                .groupBy(UserTable.id)
                .orderBy(UserTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
                .mapLazy(MarketplaceArtistEntity::wrapRow)
        }

        private fun ArtistFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
            ops += SongTable.archived eq false
            ops += SongTable.mintingStatus eq MintingStatus.Released
            olderThan?.let {
                ops += UserTable.createdAt less it
            }
            newerThan?.let {
                ops += UserTable.createdAt greater it
            }
            ids?.includes?.let {
                ops += UserTable.id inList it
            }
            ids?.excludes?.let {
                ops += UserTable.id notInList it
            }
            genres?.includes?.let {
                ops += UserTable.genre inList it
            }
            genres?.excludes?.let {
                ops += UserTable.genre notInList it
            }
            return ops
        }
    }
}
