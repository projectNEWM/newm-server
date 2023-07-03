package io.newm.server.features.distribution.eveara

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.common.truth.Truth.assertThat
import io.ktor.server.application.ApplicationEnvironment
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_NEWM_EMAIL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID
import io.newm.server.config.repo.ConfigRepositoryImpl
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.collaboration.repo.CollaborationRepositoryImpl
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.server.ktx.getFileNameWithExtensionFromUrl
import io.newm.server.ktx.toAudioContentType
import io.newm.server.ktx.toBucketAndKey
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EvearaDistributionRepositoryTest : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            UserTable.deleteAll()
        }
    }

    private lateinit var songId: UUID

    private suspend fun setupDatabase() {
        val primaryArtistId = listOf(
            User(
                firstName = "Danketsu",
                lastName = "",
                genre = "House",
                role = "Artist",
                email = "danketsu@me.com",
                nickname = "Danketsu",
                websiteUrl = "https://danketsu.io",
                instagramUrl = "https://instagram.com/danketsu",
                twitterUrl = "https://twitter.com/danketsu",
            ),
            User(
                firstName = "Mirai",
                lastName = "Music",
                genre = "House",
                role = "Artist",
                email = "mirai@me.com",
                nickname = "Mirai Music",
                websiteUrl = "https://miraimusicproductions.com",
            ),
            User(
                firstName = "Ashley",
                lastName = "Nastasia Griffin",
                genre = "House",
                role = "Artist",
                email = "NSTASIA@me.com",
                nickname = "NSTASIA",
                websiteUrl = "https://www.nstasia.com",
                twitterUrl = "https://twitter.com/nstasia",
            ),
            User(
                firstName = "Bob",
                lastName = "Ross",
                role = "Artwork",
                email = "bob.ross@pbs.org",
            ),
            User(
                firstName = "Aaron",
                lastName = "Ortiz",
                role = "Author (Lyrics)",
                email = "lyrics1@domain.com",
            ),
            User(
                firstName = "Aled",
                lastName = "Williams",
                role = "Author (Lyrics)",
                email = "lyrics2@domain.com",
            ),
            User(
                firstName = "Kahmeil",
                lastName = "Brown",
                role = "Author (Lyrics)",
                email = "lyrics3@domain.com",
            ),
            User(
                firstName = "Elijah",
                lastName = "Boac",
                role = "Author (Lyrics)",
                email = "lyrics4@domain.com",
            ),
            User(
                firstName = "Tommy",
                lastName = "Bui",
                role = "Author (Lyrics)",
                email = "lyrics5@domain.com",
            ),
            User(
                firstName = "Kyle",
                lastName = "Kim",
                role = "Author (Lyrics)",
                email = "lyrics6@domain.com",
            ),
            User(
                firstName = "Riccardo",
                lastName = "Lovatto",
                role = "Author (Lyrics)",
                email = "lyrics7@domain.com",
            ),
            User(
                firstName = "Jeff",
                lastName = "MacMillan",
                role = "Synthesizer",
                email = "jeff@macmillan.io",
            )
        ).map { user -> addUserToDatabase(user) }.first()

        songId = addSongToDatabase(
            Song(
                ownerId = primaryArtistId,
                title = "Daisuke",
                genres = listOf("Pop", "House", "Tribal"),
                releaseDate = LocalDate.parse("2023-02-03"),
                publicationDate = LocalDate.parse("2023-02-03"),
                // isrc = "QZ-NW7-23-57511",
                moods = listOf("spiritual"),
                coverArtUrl = "https://res.cloudinary.com/newm/image/upload/c_fit,w_4000,h_4000/v1683539164/ufshvmlfxbis0ba4bshw.jpg",
                arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
                arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                album = "Daisuke",
                duration = 200000,
                track = 1,
                copyright = "© Mirai Music Publishing, ℗ Danketsu, Mirai Music, NSTASIA",
                originalAudioUrl = "s3://garageaudiotranscoders3s-audiotranscoderinputbuck-873y4j5zz15i/cabb57b8-89f0-476f-8e95-e5c7a7d992c6/Vibrate.flac",
                mintingStatus = MintingStatus.Pending
            )
        )

        listOf(
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "bob.ross@pbs.org",
                role = "Artwork",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "NSTASIA@me.com",
                role = "Artist",
                royaltyRate = 10.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "mirai@me.com",
                role = "Artist",
                royaltyRate = 10.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics1@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics2@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics3@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics4@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics5@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics6@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "lyrics7@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "NSTASIA@me.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "jeff@macmillan.io",
                role = "Synthesizer",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = songId,
                email = "mirai@me.com",
                role = "Producer",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
        ).forEach { collab -> addCollabToDatabase(collab) }

        addConfigToDatabase(CONFIG_KEY_EVEARA_NEWM_EMAIL, "accounting@newm.io")
        addConfigToDatabase(CONFIG_KEY_EVEARA_PARTNER_SUBSCRIPTION_ID, "570")
    }

    @Test
    @Disabled
    fun `test distributing a song`() = runBlocking {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = Level.INFO

        val env = System.getenv()
        root.info { "AWS_ACCESS_KEY_ID: ${env["AWS_ACCESS_KEY_ID"]}" }
        root.info { "AWS_SECRET_ACCESS_KEY: ${env["AWS_SECRET_ACCESS_KEY"]}" }

        setupDatabase()

        val configRepository: ConfigRepository = ConfigRepositoryImpl()
        val songRepository: SongRepository = SongRepositoryImpl(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val applicationEnvironment: ApplicationEnvironment by inject()
        val collabRepository: CollaborationRepository =
            CollaborationRepositoryImpl(applicationEnvironment, mockk())
        val distributionRepository: DistributionRepository =
            EvearaDistributionRepositoryImpl(collabRepository, configRepository)

        val song = songRepository.get(songId)

        distributionRepository.distributeSong(song)
    }

    @Test
    fun `test s3 url parsing`() {
        val s3Url =
            "s3://garageaudiotranscoders3s-audiotranscoderinputbuck-873y4j5zz15i/cabb57b8-89f0-476f-8e95-e5c7a7d992c6/Vibrate.flac"
        assertThat(s3Url.getFileNameWithExtensionFromUrl()).isEqualTo("Vibrate.flac")
        val (bucket, key) = s3Url.toBucketAndKey()
        assertThat(bucket).isEqualTo("garageaudiotranscoders3s-audiotranscoderinputbuck-873y4j5zz15i")
        assertThat(key).isEqualTo("cabb57b8-89f0-476f-8e95-e5c7a7d992c6/Vibrate.flac")
        assertThat(s3Url.toAudioContentType()).isEqualTo("audio/x-flac")
    }
}
