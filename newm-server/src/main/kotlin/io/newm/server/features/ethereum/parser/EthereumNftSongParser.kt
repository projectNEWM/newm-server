package io.newm.server.features.ethereum.parser

import io.ktor.utils.io.core.toByteArray
import io.newm.server.features.ethereum.model.EthereumNft
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.shared.ktx.containsIgnoreCase
import java.util.UUID

private val TITLE_TRAIT_TYPES = listOf(
    "song",
    "title",
    "track",
    "song title",
    "song name",
    "track name",
    "composition",
    "layer name"
)

private val ARTIST_TRAIT_TYPES = listOf(
    "artist",
    "creator",
    "collaborator",
    "performer",
    "layer artist",
    "composer"
)

private val DURATION_TRAIT_TYPES = listOf(
    "duration",
    "length",
    "track length",
    "runtime",
    "layer duration"
)

private val GENRE_TRAIN_TYPES = listOf(
    "genre",
    "music genre",
    "style",
    "track genre",
    "layer genre"
)

private val MOOD_TRAIT_TYPES = listOf(
    "mood",
    "vibe",
    "emotion",
    "layer mood"
)

private val AUDIO_URL_REGEX = "\\.(mp3|mp4|wav|flac|aiff|aac)$".toRegex(RegexOption.IGNORE_CASE)

fun EthereumNft.parseSong(): EthereumNftSong? {
    val audioUrl = (raw?.metadata?.animationUrl ?: animation?.cachedUrl ?: animation?.originalUrl)?.asHttpsUrl()
    return if (audioUrl != null && (audioUrl.matches(AUDIO_URL_REGEX) || animation?.contentType?.startsWith("audio/") == true)) {
        EthereumNftSong(
            id = UUID.nameUUIDFromBytes(contract.address.toByteArray() + tokenId.toByteArray()),
            contractAddress = contract.address,
            tokenType = tokenType,
            tokenId = tokenId,
            amount = balance,
            title = trait(TITLE_TRAIT_TYPES) ?: raw?.metadata?.name ?: contract.name ?: name.orEmpty(),
            imageUrl = (raw?.metadata?.image ?: image?.cachedUrl ?: image?.originalUrl)?.asHttpsUrl().orEmpty(),
            audioUrl = audioUrl,
            duration = trait(DURATION_TRAIT_TYPES)?.asDuration() ?: -1,
            artists = traits(ARTIST_TRAIT_TYPES),
            genres = traits(GENRE_TRAIN_TYPES),
            moods = traits(MOOD_TRAIT_TYPES)
        )
    } else {
        // not a music NFT
        null
    }
}

private fun EthereumNft.trait(types: List<String>): String? =
    raw
        ?.metadata
        ?.attributes
        ?.firstOrNull { types.containsIgnoreCase(it.traitType) }
        ?.value

private fun EthereumNft.traits(types: List<String>): List<String> =
    raw
        ?.metadata
        ?.attributes
        ?.filter { types.containsIgnoreCase(it.traitType) }
        ?.map { it.value }
        .orEmpty()

private fun String.asHttpsUrl(): String? =
    when {
        startsWith("https://") -> this
        startsWith("ipfs://") -> "https://ipfs.io/ipfs/${substring(7)}"
        startsWith("ar://") -> "https://arweave.net/${substring(5)}"
        else -> null
    }

private fun String.asDuration(): Long {
    val parts = split(":")
    return try {
        when (parts.size) {
            1 -> parts[0].toLong() // seconds
            2 -> 60 * parts[0].toLong() + parts[1].toLong() // mm:ss format
            else -> -1L
        }
    } catch (e: NumberFormatException) {
        -1L
    }
}
