package io.newm.server.features.song.model

enum class ReleaseType {
    /**
     * The default release type if it doesn't fall into any other category.
     */
    ALBUM,

    /**
     * Only one track is allowed.
     */
    SINGLE,

    /**
     * The release should have more than four main artists.
     */
    COMPILATION_ALBUM,

    /**
     * Combined tracks duration cannot exceed 30 minutes.
     * Can add only up to six tracks.
     * If number of tracks are three or less, at least one track should be 10 minutes long.
     */
    EP,
}
