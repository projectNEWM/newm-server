package io.newm.server.features.arweave.ktx
import com.google.common.truth.Truth.assertThat
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class SongExtTest : KoinTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // initialize Koin dependency injection for tests
            startKoin {
                modules(
                    module {
                        // inject mocks
                        single<Logger> { LoggerFactory.getLogger("SongExtTest") }
                    }
                )
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            // close Koin
            stopKoin()
        }
    }

    @Test
    fun `test should return empty list`() =
        runTest {
            val song =
                Song(
                    ownerId = UUID.randomUUID(),
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
                    duration = 200000,
                    track = 1,
                    compositionCopyrightOwner = "Mirai Music Publishing",
                    compositionCopyrightYear = 2023,
                    phonographicCopyrightOwner = "Danketsu, Mirai Music, NSTASIA",
                    phonographicCopyrightYear = 2023,
                    mintingStatus = MintingStatus.Pending
                )

            val actual: List<Pair<String, String>> = song.toFiles()
            assertThat(actual).isEqualTo(listOfNotNull(null))
        }

    @Test
    fun `test should return non empty list`() =
        runTest {
            val song =
                Song(
                    ownerId = UUID.randomUUID(),
                    title = "Daisuke",
                    genres = listOf("Pop", "House", "Tribal"),
                    releaseDate = LocalDate.parse("2023-02-03"),
                    publicationDate = LocalDate.parse("2023-02-03"),
                    isrc = "QZ-NW7-23-57511",
                    moods = listOf("spiritual"),
                    arweaveCoverArtUrl = "ar://GlMlqHIPjwUtlPUfQxDdX1jWSjlKK1BCTBIekXgA66A",
                    arweaveLyricsUrl = "ar://7vQTHTkgybn8nVLDlukGiBazy2NZVhWP6HZdJdmPH00",
                    arweaveClipUrl = "ar://QpgjmWmAHNeRVgx_Ylwvh16i3aWd8BBgyq7f16gaUu0",
                    tokenAgreementUrl = "https://newm.io/agreement",
                    album = "Daisuke",
                    duration = 200000,
                    track = 1,
                    compositionCopyrightOwner = "Mirai Music Publishing",
                    compositionCopyrightYear = 2023,
                    phonographicCopyrightOwner = "Danketsu, Mirai Music, NSTASIA",
                    phonographicCopyrightYear = 2023,
                    mintingStatus = MintingStatus.Pending
                )

            val actual = song.toFiles()
            assertThat(actual).isEqualTo(listOf(Pair("https://newm.io/agreement", "application/pdf")))
        }
}
