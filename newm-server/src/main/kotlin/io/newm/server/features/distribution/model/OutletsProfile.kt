package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutletsProfile(
    /**
     * Expecting a valid Spotify artist profile URI (Eg: spotify:artist:#############).
     * Artist name (name) should match with the name in Spotify profile.
     */
    @SerialName("spotify_profile")
    val spotifyProfile: String? = null,

    /**
     * Expecting a valid SoundCloud artist profile permalink (Eg: for "https://soundcloud.com/#############" just include "#############")
     * Artist name (name) should match with the name in SoundCloud profile.
     */
    @SerialName("soundcloudgo_profile")
    val soundcloudgoProfile: String? = null,

    /**
     * Expecting a valid Apple artist profile id (Eg: for "https://music.apple.com/us/artist/abcdefgh/#############" just include "#############").
     * Artist name (name) should match with the name in Apple Music profile.
     */
    @SerialName("applemusic_profile")
    val applemusicProfile: String? = null,
)
