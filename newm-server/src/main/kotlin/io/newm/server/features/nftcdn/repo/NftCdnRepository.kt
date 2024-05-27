package io.newm.server.features.nftcdn.repo

interface NftCdnRepository {
    fun generateImageUrl(fingerprint: String): String

    fun generateFileUrl(
        fingerprint: String,
        index: Int
    ): String

    fun generateUrl(
        fingerprint: String,
        path: String,
    ): String
}
