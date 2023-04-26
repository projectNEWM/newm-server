package io.newm.server.features.minting.repo

import com.google.common.annotations.VisibleForTesting
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.grpc.plutusDataMap
import io.newm.chain.grpc.plutusDataMapItem
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationFilters
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.shared.koin.inject
import io.newm.txbuilder.ktx.toPlutusData
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import kotlin.time.Duration.Companion.seconds

class MintingRepositoryImpl(
    private val userRepository: UserRepository,
    private val collabRepository: CollaborationRepository,
    private val cardanoRepository: CardanoRepository,
    private val configRepository: ConfigRepository,
) : MintingRepository {

    private val log: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun mint(song: Song) {
        val user = userRepository.get(song.ownerId!!)
        val collabs = collabRepository.getAll(
            user.id!!,
            CollaborationFilters(
                songIds = listOf(song.id!!),
                olderThan = null,
                newerThan = null,
                ids = null,
                emails = null
            ),
            0,
            Integer.MAX_VALUE
        )
        val cip68Metadata = buildStreamTokenMetadata(song, user, collabs)
        val paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
        val mintPriceLovelace = configRepository.getString("mint.price")
        val paymentUtxo = cardanoRepository.queryLiveUtxos(paymentKey.address)
            .first { it.lovelace == mintPriceLovelace && it.nativeAssetsCount == 0 }
        val cashRegisterKey =
            requireNotNull(cardanoRepository.getKeyByName("cashRegister")) { "cashRegister key not defined!" }
        val cashRegisterUtxos = cardanoRepository.queryLiveUtxos(cashRegisterKey.address)
            .filter { it.nativeAssetsCount == 0 }
            .sortedByDescending { it.lovelace.toLong() }
            .take(5)
        val transactionBuilderResponse =
            buildMintingTransaction(paymentUtxo, cashRegisterUtxos, cashRegisterKey.address)
        TODO("Finish building tx and submit it.")
    }

    private suspend fun buildMintingTransaction(
        paymentUtxo: Utxo,
        cashRegisterUtxos: List<Utxo>,
        changeAddress: String,
    ) = cardanoRepository.buildTransaction {
        with(sourceUtxos) {
            add(paymentUtxo)
            addAll(cashRegisterUtxos)
        }

        this.changeAddress = changeAddress
    }

    @VisibleForTesting
    internal fun buildStreamTokenMetadata(song: Song, user: User, collabs: List<Collaboration>): PlutusData {
        return plutusData {
            constr = 0
            list = plutusDataList {
                with(listItem) {
                    add(
                        plutusData {
                            map = plutusDataMap {
                                with(mapItem) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "name".toPlutusData()
                                            mapItemValue = song.title!!.toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "image".toPlutusData()
                                            mapItemValue = song.arweaveCoverArtUrl!!.toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "mediaType".toPlutusData()
                                            mapItemValue = "image/webp".toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "music_metadata_version".toPlutusData()
                                            mapItemValue = plutusData { int = 2 } // CIP-60 Version 2
                                        }
                                    )
                                    add(createPlutusDataRelease(song, collabs))
                                    add(createPlutusDataFiles(song, user, collabs))
                                    add(createPlutusDataLinks(user, collabs))
                                }
                            }
                        }
                    )
                    // CIP-68 Version
                    add(plutusData { int = 1L })
                }
            }
        }
    }

    private fun createPlutusDataRelease(song: Song, collabs: List<Collaboration>) = plutusDataMapItem {
        mapItemKey = "release".toPlutusData()
        mapItemValue = plutusData {
            map = plutusDataMap {
                with(mapItem) {
                    add(
                        plutusDataMapItem {
                            mapItemKey = "release_type".toPlutusData()
                            mapItemValue =
                                "Single".toPlutusData() // All Singles for now
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "release_title".toPlutusData()
                            mapItemValue = song.album!!.toPlutusData()
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "release_date".toPlutusData()
                            mapItemValue = song.releaseDate!!.toString().toPlutusData()
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "publication_date".toPlutusData()
                            mapItemValue = song.publicationDate!!.toString().toPlutusData()
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "distributor".toPlutusData()
                            mapItemValue = "https://newm.io".toPlutusData()
                        }
                    )
                    val visualArtists =
                        collabs.filter { it.role.equals("Artwork", ignoreCase = true) && it.credited == true }
                    if (visualArtists.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "visual_artist".toPlutusData()
                                    mapItemValue = if (visualArtists.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(
                                                    visualArtists.map { collab ->
                                                        UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).fullName
                                                            .toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        UserEntity.getByEmail(visualArtists[0].email!!)!!
                                            .toModel(false).fullName.toPlutusData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createPlutusDataFiles(song: Song, user: User, collabs: List<Collaboration>) = plutusDataMapItem {
        mapItemKey = "files".toPlutusData()
        mapItemValue = plutusData {
            list = plutusDataList {
                with(listItem) {
                    add(
                        plutusData {
                            map = plutusDataMap {
                                with(mapItem) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "name".toPlutusData()
                                            mapItemValue = "Streaming Royalty Share Agreement".toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "mediaType".toPlutusData()
                                            mapItemValue = "application/pdf".toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "src".toPlutusData()
                                            mapItemValue = song.arweaveTokenAgreementUrl!!.toPlutusData()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    add(
                        plutusData {
                            map = plutusDataMap {
                                with(mapItem) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "name".toPlutusData()
                                            mapItemValue = song.title!!.toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "mediaType".toPlutusData()
                                            mapItemValue = "audio/mpeg".toPlutusData()
                                        }
                                    )
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "src".toPlutusData()
                                            mapItemValue = song.arweaveClipUrl!!.toPlutusData()
                                        }
                                    )
                                    add(createPlutusDataSong(song, user, collabs))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun createPlutusDataSong(song: Song, user: User, collabs: List<Collaboration>) = plutusDataMapItem {
        mapItemKey = "song".toPlutusData()
        mapItemValue = plutusData {
            map = plutusDataMap {
                with(mapItem) {
                    add(
                        plutusDataMapItem {
                            mapItemKey = "song_title".toPlutusData()
                            mapItemValue = song.title!!.toPlutusData()
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "song_duration".toPlutusData()
                            mapItemValue =
                                song.duration!!.seconds.toIsoString().toPlutusData()
                        }
                    )
                    add(
                        plutusDataMapItem {
                            mapItemKey = "track_number".toPlutusData()
                            mapItemValue = 1.toPlutusData()
                        }
                    )
                    song.moods?.takeUnless { it.isEmpty() }?.let { moods ->
                        add(
                            plutusDataMapItem {
                                mapItemKey = "mood".toPlutusData()
                                mapItemValue = if (moods.size > 1) {
                                    plutusData {
                                        list = plutusDataList {
                                            listItem.addAll(
                                                moods.map { it.toPlutusData() }
                                            )
                                        }
                                    }
                                } else {
                                    moods[0].toPlutusData()
                                }
                            }
                        )
                    }
                    add(createPlutusDataArtists(user, collabs))
                    if (!song.genres.isNullOrEmpty()) {
                        add(
                            plutusDataMapItem {
                                mapItemKey = "genres".toPlutusData()
                                mapItemValue = plutusData {
                                    list = plutusDataList {
                                        listItem.addAll(song.genres.map { it.toPlutusData() })
                                    }
                                }
                            }
                        )
                    }
                    add(
                        plutusDataMapItem {
                            mapItemKey = "copyright".toPlutusData()
                            mapItemValue = song.copyright!!.toPlutusData()
                        }
                    )
                    if (!song.arweaveLyricsUrl.isNullOrBlank()) {
                        add(
                            plutusDataMapItem {
                                mapItemKey = "lyrics".toPlutusData()
                                mapItemValue = song.arweaveLyricsUrl.toPlutusData()
                            }
                        )
                    }
                    if (!song.parentalAdvisory.isNullOrBlank()) {
                        add(
                            plutusDataMapItem {
                                mapItemKey = "parental_advisory".toPlutusData()
                                mapItemValue = song.parentalAdvisory.toPlutusData()
                            }
                        )
                        add(
                            plutusDataMapItem {
                                mapItemKey = "explicit".toPlutusData()
                                mapItemValue =
                                    (!song.parentalAdvisory.equals("Non-Explicit", ignoreCase = true)).toString()
                                        .toPlutusData()
                            }
                        )
                    }
                    add(
                        plutusDataMapItem {
                            mapItemKey = "isrc".toPlutusData()
                            mapItemValue = song.isrc!!.toPlutusData()
                        }
                    )
                    if (!song.iswc.isNullOrBlank()) {
                        add(
                            plutusDataMapItem {
                                mapItemKey = "iswc".toPlutusData()
                                mapItemValue = song.iswc.toPlutusData()
                            }
                        )
                    }
                    if (!song.ipi.isNullOrEmpty()) {
                        add(
                            plutusDataMapItem {
                                mapItemKey = "ipi".toPlutusData()
                                mapItemValue = plutusData {
                                    list = plutusDataList {
                                        listItem.addAll(song.ipi.map { it.toPlutusData() })
                                    }
                                }
                            }
                        )
                    }

                    // TODO: add country_of_origin from idenfy data? - maybe not MVP

                    val lyricists =
                        collabs.filter { it.role.equals("Author (Lyrics)", ignoreCase = true) && it.credited == true }
                    if (lyricists.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "lyricists".toPlutusData()
                                    mapItemValue = plutusData {
                                        list = plutusDataList {
                                            listItem.addAll(
                                                lyricists.map { collab ->
                                                    UserEntity.getByEmail(collab.email!!)!!
                                                        .toModel(false).fullName.toPlutusData()
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    val contributingArtists =
                        collabs.filter { it.role in contributingArtistRoles && it.credited == true }
                    if (contributingArtists.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "contributing_artists".toPlutusData()
                                    mapItemValue = plutusData {
                                        list = plutusDataList {
                                            listItem.addAll(
                                                contributingArtists.map { collab ->
                                                    "${
                                                        UserEntity.getByEmail(collab.email!!)!!.toModel(false)
                                                            .let { it.nickname ?: it.fullName }
                                                    }, ${collab.role}".toPlutusData()
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    val mixEngineers =
                        collabs.filter { it.role.equals("Mixing Engineer", ignoreCase = true) && it.credited == true }
                    if (mixEngineers.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "mix_engineer".toPlutusData()
                                    mapItemValue = if (mixEngineers.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(
                                                    mixEngineers.map { collab ->
                                                        UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).fullName
                                                            .toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        UserEntity.getByEmail(mixEngineers[0].email!!)!!
                                            .toModel(false).fullName.toPlutusData()
                                    }
                                }
                            )
                        }
                    }

                    val masteringEngineers = collabs.filter {
                        it.role.equals(
                            "Mastering Engineer",
                            ignoreCase = true
                        ) && it.credited == true
                    }
                    if (masteringEngineers.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "mastering_engineer".toPlutusData()
                                    mapItemValue = if (masteringEngineers.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(
                                                    masteringEngineers.map { collab ->
                                                        UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).fullName.toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        UserEntity.getByEmail(masteringEngineers[0].email!!)!!
                                            .toModel(false).fullName.toPlutusData()
                                    }
                                }
                            )
                        }
                    }

                    val recordingEngineers = collabs.filter {
                        it.role.equals(
                            "Recording Engineer",
                            ignoreCase = true
                        ) && it.credited == true
                    }
                    if (recordingEngineers.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "recording_engineer".toPlutusData()
                                    mapItemValue = if (recordingEngineers.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(
                                                    recordingEngineers.map { collab ->
                                                        UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).fullName.toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        UserEntity.getByEmail(recordingEngineers[0].email!!)!!
                                            .toModel(false).fullName.toPlutusData()
                                    }
                                }
                            )
                        }
                    }

                    val producers = collabs.filter {
                        it.role in producerRoles && it.credited == true
                    }
                    if (producers.isNotEmpty()) {
                        transaction {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "producer".toPlutusData()
                                    mapItemValue = if (producers.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(
                                                    producers.map { collab ->
                                                        UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).let { it.nickname ?: it.fullName }
                                                            .toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        UserEntity.getByEmail(producers[0].email!!)!!
                                            .toModel(false).fullName.toPlutusData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createPlutusDataArtists(user: User, collabs: List<Collaboration>) = plutusDataMapItem {
        mapItemKey = "artists".toPlutusData()
        mapItemValue = plutusData {
            list = plutusDataList {
                with(listItem) {
                    add(
                        plutusData {
                            map = plutusDataMap {
                                with(mapItem) {
                                    add(
                                        plutusDataMapItem {
                                            mapItemKey = "name".toPlutusData()
                                            mapItemValue = user.nickname!!.toPlutusData()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    transaction {
                        collabs.filter { it.role.equals("Artist", ignoreCase = true) && it.credited == true }
                            .forEach { collab ->
                                add(
                                    plutusData {
                                        map = plutusDataMap {
                                            with(mapItem) {
                                                add(
                                                    plutusDataMapItem {
                                                        mapItemKey = "name".toPlutusData()
                                                        mapItemValue = UserEntity.getByEmail(collab.email!!)!!
                                                            .toModel(false).nickname!!.toPlutusData()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    }
                }
            }
        }
    }

    private fun createPlutusDataLinks(user: User, collabs: List<Collaboration>) = transaction {
        plutusDataMapItem {
            val collabUsers = listOf(user) + collabs.filter { it.credited == true }
                .mapNotNull { UserEntity.getByEmail(it.email!!)?.toModel(false) }

            val websites = collabUsers.mapNotNull { it.websiteUrl }.distinct()
            val instagrams = collabUsers.mapNotNull { it.instagramUrl }.distinct()
            val twitters = collabUsers.mapNotNull { it.twitterUrl }.distinct()

            mapItemKey = "links".toPlutusData()
            mapItemValue = plutusData {
                map = plutusDataMap {
                    with(mapItem) {
                        if (websites.isNotEmpty()) {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "website".toPlutusData()
                                    mapItemValue = if (websites.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(websites.map { it.toPlutusData() })
                                            }
                                        }
                                    } else {
                                        websites[0].toPlutusData()
                                    }
                                }
                            )
                        }
                        if (instagrams.isNotEmpty()) {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "instagram".toPlutusData()
                                    mapItemValue = if (instagrams.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(instagrams.map { it.toPlutusData() })
                                            }
                                        }
                                    } else {
                                        instagrams[0].toPlutusData()
                                    }
                                }
                            )
                        }
                        if (twitters.isNotEmpty()) {
                            add(
                                plutusDataMapItem {
                                    mapItemKey = "twitter".toPlutusData()
                                    mapItemValue = if (twitters.size > 1) {
                                        plutusData {
                                            list = plutusDataList {
                                                listItem.addAll(twitters.map { it.toPlutusData() })
                                            }
                                        }
                                    } else {
                                        twitters[0].toPlutusData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        // FIXME: these should come from the eveara api and we should flag our db with which ones are contributing roles
        private val contributingArtistRoles = listOf(
            "Composer (Music)",
            "Contributor",
            "Guitar",
            "Drums",
            "Bass",
            "Keyboards",
            "Vocal",
            "Harmonica",
            "Saxophone",
            "Violin",
            "Orchestra",
            "Organ",
            "Choir",
            "Piano",
            "Horns",
            "Strings",
            "Synthesizer",
            "Percussion"
        )

        private val producerRoles = listOf(
            "Producer",
            "Executive Producer"
        )
    }
}
