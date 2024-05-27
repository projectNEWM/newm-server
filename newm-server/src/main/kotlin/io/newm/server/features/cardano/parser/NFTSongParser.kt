package io.newm.server.features.cardano.parser

import io.ktor.utils.io.core.toByteArray
import io.newm.chain.grpc.LedgerAssetMetadataItem
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.util.hexStringToAssetName
import io.newm.server.features.cardano.model.NFTSong
import io.newm.server.features.nftcdn.repo.NftCdnRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.toDurationOrNull
import io.newm.shared.ktx.warn
import io.newm.txbuilder.ktx.fingerprint
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.UUID

private val logger: Logger by inject { parametersOf("NFTSongParser") }
private val nftCdnRepository: NftCdnRepository by inject()

// Used to remove numeric prefixes on legacy SickCity NFT fields ( e.g., "1. Artist Name" and "02. Song Title")
private val legacyPrefixRegex = "^\\d+\\.\\s*".toRegex()

fun List<LedgerAssetMetadataItem>.toNFTSongs(
    asset: NativeAsset,
    isNftCdnEnabled: Boolean
): List<NFTSong> {
    val assetName = asset.name.hexStringToAssetName()
    logger.debug { "Parsing PolicyID: ${asset.policy}, assetName: $assetName" }

    var image: String? = null
    var songTitle: String? = null
    var songDuration: String? = null
    val artists = mutableSetOf<String>()
    val genres = mutableSetOf<String>()
    val moods = mutableSetOf<String>()
    val files = mutableSetOf<File>()

    val parseSongItem = { item: LedgerAssetMetadataItem ->
        when (item.key) {
            "artists" -> {
                artists += item.artists()
            }

            "genres" -> {
                genres += item.values()
            }

            "mood" -> {
                moods += item.value
            }

            "song_title" -> {
                songTitle = item.value
            }

            "song_duration" -> {
                songDuration = item.value
            }
        }
    }

    for (item in this) {
        when (item.key.replaceFirst(legacyPrefixRegex, "")) {
            "image" -> {
                image = item.value
            }

            "files" -> {
                files += item.audioFiles()
            }

            "release" -> {
                // only available if CIP60-V2 or above
                item.childrenList.forEach(parseSongItem)
            }

            "Artist", "Artist Name" -> {
                // SickCity Legacy
                artists += listOf(item.value)
            }

            "Song Title" -> {
                // SickCity Legacy
                songTitle = item.value
            }

            "Genre", "Sub-Genre" -> {
                // SickCity Legacy
                genres += item.value
            }

            else -> parseSongItem(item)
        }
    }
    val fingerprint = asset.fingerprint()
    return files.mapNotNull { file ->
        val title = file.songTitle ?: songTitle ?: file.name
        if (title == null || file.src == null || image == null) {
            logger.warn { "Skipped invalid PolicyID: ${asset.policy}, assetName: $assetName, title: $title, src: ${file.src}, image: $image" }
            null
        } else {
            NFTSong(
                id = UUID.nameUUIDFromBytes(file.src.toByteArray()),
                fingerprint = fingerprint,
                policyId = asset.policy,
                assetName = assetName,
                amount = asset.amount.toLong(),
                title = title,
                audioUrl =
                    file.src.takeUnless { isNftCdnEnabled }?.toResourceUrl() ?: nftCdnRepository.generateFileUrl(
                        fingerprint = fingerprint,
                        index = file.index
                    ),
                imageUrl =
                    image.takeUnless { isNftCdnEnabled }?.toResourceUrl() ?: nftCdnRepository.generateImageUrl(
                        fingerprint = fingerprint
                    ),
                duration = (file.songDuration ?: songDuration)?.toDurationOrNull()?.toSeconds() ?: -1L,
                artists = file.artists ?: artists.toList(),
                genres = file.genres ?: genres.toList(),
                moods = file.mood?.let(::listOf) ?: moods.toList()
            )
        }
    }
}

private operator fun List<LedgerAssetMetadataItem>.get(key: String): LedgerAssetMetadataItem? = firstOrNull { it.key == key }

private fun List<LedgerAssetMetadataItem>.value(key: String): String? = this[key]?.value

private fun LedgerAssetMetadataItem.values(): List<String> = childrenList?.map { it.value }.orEmpty()

private fun LedgerAssetMetadataItem.artists(): List<String> =
    childrenList?.mapNotNull { it.childrenList.value("name") }.orEmpty()
        .ifEmpty { values() }
        .ifEmpty { value?.let(::listOf) }.orEmpty()

private fun LedgerAssetMetadataItem.audioFiles(): List<File> {
    val files = mutableListOf<File>()
    childrenList?.forEachIndexed { index, child ->
        if (child.isAudioFile) {
            files += child.childrenList.toFile(index)
        }
    }
    return files
}

private val LedgerAssetMetadataItem.isAudioFile: Boolean
    get() = childrenList.value("mediaType")?.startsWith("audio/") == true

private data class File(
    val index: Int,
    val name: String?,
    val src: String?,
    var songTitle: String?,
    var songDuration: String?,
    val artists: List<String>?,
    val genres: List<String>?,
    val mood: String?
)

private fun List<LedgerAssetMetadataItem>.toFile(index: Int): File {
    // "song" only available if CIP60-V2 or above
    val song = this["song"]?.childrenList ?: this
    return File(
        index = index,
        name = value("name"),
        src = value("src"),
        songTitle = song.value("song_title"),
        songDuration = song.value("song_duration"),
        artists = song["artists"]?.artists(),
        genres = song["genres"]?.values(),
        mood = song.value("mood")
    )
}

internal fun String.toResourceUrl(): String =
    when {
        startsWith("ipfs://") -> "https://ipfs.io/ipfs/${substring(7)}"
        startsWith("ar://") -> "https://arweave.net/${substring(5)}"
        else -> {
            logger.warn { "Unknown resource schema: $this" }
            this
        }
    }
