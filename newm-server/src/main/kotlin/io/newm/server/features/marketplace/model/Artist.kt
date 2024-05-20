package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.UserId
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Artist(
    @Serializable(with = UUIDSerializer::class)
    val id: UserId,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val name: String,
    val genre: String?,
    val location: String?,
    var biography: String?,
    val pictureUrl: String?,
    val websiteUrl: String?,
    val twitterUrl: String?,
    val instagramUrl: String?,
    val spotifyProfile: String?,
    val soundCloudProfile: String?,
    val appleMusicProfile: String?,
    val releasedSongCount: Long?,
    val marketplaceSongCount: Long?
)
