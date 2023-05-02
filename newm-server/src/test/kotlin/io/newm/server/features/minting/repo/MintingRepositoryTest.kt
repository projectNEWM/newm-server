package io.newm.server.features.minting.repo

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.shared.ktx.toHexString
import io.newm.shared.ktx.toUUID
import io.newm.txbuilder.ktx.toCborObject
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MintingRepositoryTest : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            UserTable.deleteAll()
        }
    }

    @Test
    fun `buildStreamTokenMetadata test`() = runBlocking {
        val primaryArtist = User(
            firstName = "Danketsu",
            lastName = "",
            genre = "House",
            role = "Artist",
            email = "danketsu@me.com",
            nickname = "Danketsu",
            websiteUrl = "https://danketsu.io",
            instagramUrl = "https://instagram.com/danketsu",
            twitterUrl = "https://twitter.com/danketsu",
        )
        val primaryArtistId = addUserToDatabase(primaryArtist)

        val artist2 = User(
            firstName = "Mirai",
            lastName = "Music",
            genre = "House",
            role = "Artist",
            email = "mirai@me.com",
            nickname = "Mirai Music",
            websiteUrl = "https://miraimusicproductions.com",
        )
        addUserToDatabase(artist2)

        val artist3 = User(
            firstName = "Ashley",
            lastName = "Nastasia Griffin",
            genre = "House",
            role = "Artist",
            email = "NSTASIA@me.com",
            nickname = "NSTASIA",
            websiteUrl = "https://www.nstasia.com",
            twitterUrl = "https://twitter.com/nstasia",
        )
        addUserToDatabase(artist3)

        val visualArtist = User(
            firstName = "Bob",
            lastName = "Ross",
            role = "Artwork",
            email = "bob.ross@pbs.org",
        )
        addUserToDatabase(visualArtist)

        val lyricist1 = User(
            firstName = "Aaron",
            lastName = "Ortiz",
            role = "Author (Lyrics)",
            email = "lyrics1@domain.com",
        )
        addUserToDatabase(lyricist1)
        val lyricist2 = User(
            firstName = "Aled",
            lastName = "Williams",
            role = "Author (Lyrics)",
            email = "lyrics2@domain.com",
        )
        addUserToDatabase(lyricist2)
        val lyricist3 = User(
            firstName = "Kahmeil",
            lastName = "Brown",
            role = "Author (Lyrics)",
            email = "lyrics3@domain.com",
        )
        addUserToDatabase(lyricist3)
        val lyricist4 = User(
            firstName = "Elijah",
            lastName = "Boac",
            role = "Author (Lyrics)",
            email = "lyrics4@domain.com",
        )
        addUserToDatabase(lyricist4)
        val lyricist5 = User(
            firstName = "Tommy",
            lastName = "Bui",
            role = "Author (Lyrics)",
            email = "lyrics5@domain.com",
        )
        addUserToDatabase(lyricist5)
        val lyricist6 = User(
            firstName = "Kyle",
            lastName = "Kim",
            role = "Author (Lyrics)",
            email = "lyrics6@domain.com",
        )
        addUserToDatabase(lyricist6)
        val lyricist7 = User(
            firstName = "Riccardo",
            lastName = "Lovatto",
            role = "Author (Lyrics)",
            email = "lyrics7@domain.com",
        )
        addUserToDatabase(lyricist7)
        val contributingArtist = User(
            firstName = "Jeff",
            lastName = "MacMillan",
            role = "Synthesizer",
            email = "jeff@macmillan.io",
        )
        addUserToDatabase(contributingArtist)

        val collabs = listOf(
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "bob.ross@pbs.org",
                role = "Artwork",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "NSTASIA@me.com",
                role = "Artist",
                royaltyRate = 10.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "mirai@me.com",
                role = "Artist",
                royaltyRate = 10.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics1@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics2@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics3@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics4@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics5@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics6@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "lyrics7@domain.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "NSTASIA@me.com",
                role = "Author (Lyrics)",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "jeff@macmillan.io",
                role = "Synthesizer",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
            Collaboration(
                id = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                songId = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
                email = "mirai@me.com",
                role = "Producer",
                royaltyRate = 0.0f,
                credited = true,
                status = CollaborationStatus.Accepted,
            ),
        )

        val collabRepository: CollaborationRepository = mockk {
            coEvery { getAll(any(), any(), any(), any()) } returns collabs
        }

        val song = Song(
            id = "1097eef1-f48f-4d18-92c1-7e58aff85bc5".toUUID(),
            ownerId = primaryArtistId,
            title = "Daisuke",
            genres = listOf("Pop", "House", "Tribal"),
            releaseDate = LocalDate.parse("2023-02-03"),
            publicationDate = LocalDate.parse("2023-02-03"),
            isrc = "QZ-NW7-23-57511",
            moods = listOf("spiritual"),
            arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
            arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
            arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
            arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
            album = "Daisuke",
            duration = 200,
            track = 1,
            copyright = "© Mirai Music Publishing, ℗ Danketsu, Mirai Music, NSTASIA",
            mintingStatus = MintingStatus.Pending
        )

        val mintingRepository = MintingRepositoryImpl(mockk(), collabRepository, mockk(), mockk())

        val plutusDataHex =
            mintingRepository.buildStreamTokenMetadata(song, primaryArtist, collabs).toCborObject().toCborByteArray()
                .toHexString()

        assertThat(plutusDataHex).isEqualTo(
            "d87982a7446e616d654744616973756b6545696d616765583061723a2f2f476c4d6c714849506a7755746c5055665178446458316a57536a6c4b4b314243544249656b586741363641496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a64c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654744616973756b654c72656c656173655f646174654a323032332d30322d3033507075626c69636174696f6e5f646174654a323032332d30322d30334b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4d76697375616c5f61727469737448426f6220526f73734566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f654b3867415043764a2d396b626950335072534d774c47416b3338614e7978504475647a7a624779707845a4446e616d654744616973756b65496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f5170676a6d576d41484e65525667785f596c7776683136693361576438424267797137663136676155753044736f6e67ac4a736f6e675f7469746c654744616973756b654d736f6e675f6475726174696f6e475054334d3230534c747261636b5f6e756d62657201446d6f6f644973706972697475616c476172746973747383a1446e616d654844616e6b65747375a1446e616d65474e535441534941a1446e616d654b4d69726169204d757369634667656e7265738343506f7045486f7573654654726962616c49636f70797269676874583dc2a9204d69726169204d75736963205075626c697368696e672c20e284972044616e6b657473752c204d69726169204d757369632c204e535441534941466c7972696373583061723a2f2f3776515448546b6779626e386e564c446c756b476942617a79324e5a5668575036485a644a646d5048303044697372634f515a2d4e57372d32332d3537353131496c7972696369737473884b4161726f6e204f7274697a4d416c65642057696c6c69616d734d4b61686d65696c2042726f776e4b456c696a616820426f616349546f6d6d7920427569484b796c65204b696d50526963636172646f204c6f766174746f574173686c6579204e61737461736961204772696666696e54636f6e747269627574696e675f6172746973747381581b4a656666204d61634d696c6c616e2c2053796e74686573697a65724870726f64756365724b4d69726169204d75736963456c696e6b73a34777656273697465835368747470733a2f2f64616e6b657473752e696f5768747470733a2f2f7777772e6e7374617369612e636f6d582168747470733a2f2f6d697261696d7573696370726f64756374696f6e732e636f6d49696e7374616772616d581e68747470733a2f2f696e7374616772616d2e636f6d2f64616e6b65747375477477697474657282581c68747470733a2f2f747769747465722e636f6d2f64616e6b65747375581b68747470733a2f2f747769747465722e636f6d2f6e73746173696101"
        )

        Unit
    }
}
