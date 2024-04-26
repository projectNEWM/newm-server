package io.newm.server.features.arweave.ktx

import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.ktx.asValidUrl
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger

private val IMAGE_WEBP_REPLACE_REGEX = Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)\$", RegexOption.IGNORE_CASE)

fun Song.toFiles(release: Release): List<Pair<String, String>> {
    val log: Logger by inject { parametersOf(javaClass.simpleName) }

    return listOfNotNull(
        if (release.arweaveCoverArtUrl != null) {
            log.info { "Release ${release.id} already has arweave cover art url: $release.arweaveCoverArtUrl" }
            null
        } else {
            coverArtUrl.asValidUrl().replace(IMAGE_WEBP_REPLACE_REGEX, ".webp") to "image/webp"
        },
        if (arweaveTokenAgreementUrl != null) {
            log.info { "Song $id already has arweave token agreement url: $arweaveTokenAgreementUrl" }
            null
        } else {
            tokenAgreementUrl!! to "application/pdf"
        },
        if (arweaveClipUrl != null) {
            log.info { "Song $id already has arweave clip url: $arweaveClipUrl" }
            null
        } else {
            clipUrl!! to "audio/mpeg"
        },
        if (arweaveLyricsUrl != null) {
            log.info { "Song $id already has arweave lyrics url: $arweaveLyricsUrl" }
            null
        } else {
            lyricsUrl?.let { it to "text/plain" }
        }
    )
}
