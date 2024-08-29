package io.newm.server.features.earnings

import com.google.common.truth.Truth
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.newm.server.BaseApplicationTests
import io.newm.server.features.earnings.database.EarningsTable
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.model.GetEarningsResponse
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EarningsRoutesTests : BaseApplicationTests() {
    @BeforeEach
    fun beforeEach() {
        transaction {
            SongTable.deleteAll()
            UserTable.deleteAll()
            EarningsTable.deleteAll()
        }
    }

    @Test
    fun testGetEarning() =
        runBlocking {
            val testStakeAddress1 = "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv"

            val user = User(
                firstName = "Tommy",
                lastName = "Bui",
                role = "Author (Lyrics)",
                email = "lyrics5@domain.com",
            )

            val userAdded = addUserToDatabase(user)

            val songId1 =
                addSongToDatabase(
                    Release(
                        ownerId = userAdded,
                        title = "Daisuke",
                        releaseType = ReleaseType.SINGLE,
                        arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                        releaseDate = LocalDate.parse("2023-02-03"),
                        publicationDate = LocalDate.parse("2023-02-03"),
                    ),
                    Song(
                        ownerId = userAdded,
                        title = "Daisuke",
                        genres = listOf("Pop", "House", "Tribal"),
                        isrc = "QZ-NW7-23-57511",
                        moods = listOf("spiritual"),
                        arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                        arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
                        arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                        duration = 200000,
                        track = 1,
                        compositionCopyrightOwner = "Mirai Music Publishing",
                        compositionCopyrightYear = 2023,
                        phonographicCopyrightOwner = "Danketsu, Mirai Music, NSTASIA",
                        phonographicCopyrightYear = 2023,
                        mintingStatus = MintingStatus.Pending
                    )
                )

            val songId2 =
                addSongToDatabase(
                    Release(
                        ownerId = userAdded,
                        title = "Sunset",
                        releaseType = ReleaseType.SINGLE,
                        arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                        releaseDate = LocalDate.parse("2023-02-03"),
                        publicationDate = LocalDate.parse("2023-02-03"),
                    ),
                    Song(
                        ownerId = userAdded,
                        title = "Sunset",
                        genres = listOf("Pop", "House", "Tribal"),
                        isrc = "QZ-NW7-23-57511",
                        moods = listOf("spiritual"),
                        arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                        arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
                        arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                        duration = 200000,
                        track = 1,
                        compositionCopyrightOwner = "Mirai Music Publishing",
                        compositionCopyrightYear = 2023,
                        phonographicCopyrightOwner = "Sunset, Mirai Music, NSTASIA",
                        phonographicCopyrightYear = 2023,
                        mintingStatus = MintingStatus.Pending
                    )
                )

            val songId3 =
                addSongToDatabase(
                    Release(
                        ownerId = userAdded,
                        title = "Sunrise",
                        releaseType = ReleaseType.SINGLE,
                        arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                        releaseDate = LocalDate.parse("2023-02-03"),
                        publicationDate = LocalDate.parse("2023-02-03"),
                    ),
                    Song(
                        ownerId = userAdded,
                        title = "Sunrise",
                        genres = listOf("Pop", "House", "Tribal"),
                        isrc = "QZ-NW7-23-57511",
                        moods = listOf("spiritual"),
                        arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                        arweaveTokenAgreementUrl = "ar://eK8gAPCvJ-9kbiP3PrSMwLGAk38aNyxPDudzzbGypxE",
                        arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                        duration = 200000,
                        track = 1,
                        compositionCopyrightOwner = "Mirai Music Publishing",
                        compositionCopyrightYear = 2023,
                        phonographicCopyrightOwner = "Sunrise, Mirai Music, NSTASIA",
                        phonographicCopyrightYear = 2023,
                        mintingStatus = MintingStatus.Pending
                    )
                )

            // Add Earnings directly into database
            val createdAt = LocalDateTime.now()
            val earning1 = addEarningsToDatabase(songId1, testStakeAddress1, 200, createdAt)
            val earning2 = addEarningsToDatabase(songId2, testStakeAddress1, 300, createdAt)
            val earning3 = addEarningsToDatabase(songId3, testStakeAddress1, 500, createdAt)

            val expected = GetEarningsResponse(
                totalClaimed = 1000,
                earnings = listOf(
                    Earning(id = earning1, amount = 200, stakeAddress = testStakeAddress1, memo = "default", createdAt = createdAt),
                    Earning(id = earning2, amount = 300, stakeAddress = testStakeAddress1, memo = "default", createdAt = createdAt),
                    Earning(id = earning3, amount = 500, stakeAddress = testStakeAddress1, memo = "default", createdAt = createdAt),
                )
            )

            // Get it
            val response =
                client.get("v1/earnings/$testStakeAddress1") {
                    bearerAuth(testUserToken)
                    accept(ContentType.Application.Json)
                }

            Truth.assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualEarning = response.body<GetEarningsResponse>()
            Truth.assertThat(actualEarning).isEqualTo(expected)
        }
}
