package io.newm.server.features.song.model

enum class ReleaseType(val value: String) {
    /**
     * The default release type if it doesn't fall into any other category.
     */
    ALBUM("album"),

    /**
     * Only one track is allowed.
     */
    SINGLE("single"),

    /**
     * The release should have more than four main artists.
     */
    COMPILATION_ALBUM("compilation_album"),

    /**
     * Combined tracks duration cannot exceed 30 minutes.
     * Can add only up to six tracks.
     * If number of tracks are three or less, at least one track should be 10 minutes long.
     */
    EP("ep"),
}
