package io.newm.server.features.marketplace.model

import io.newm.server.typealiases.UserId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Artist(
    @Contextual
    val id: UserId,
    @Contextual
    val createdAt: LocalDateTime,
    val name: String,
    val genre: String?,
    val location: String?,
    var biography: String?,
    val pictureUrl: String?,
    val bannerUrl: String?,
    val websiteUrl: String?,
    val twitterUrl: String?,
    val instagramUrl: String?,
    val spotifyProfile: String?,
    val soundCloudProfile: String?,
    val appleMusicProfile: String?,
    val releasedSongCount: Long?,
    val marketplaceSongCount: Long?
)
