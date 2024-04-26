package io.newm.server.features.arweave.repo

import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsync
import com.google.common.truth.Truth.assertThat
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.mockk.mockk
import io.newm.server.features.arweave.model.WeaveFile
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import kotlin.math.exp

class UtilTest : KoinTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // initialize Koin dependency injection for tests
            startKoin {
                modules(
                    module {
                        // inject mocks
                        single<Logger> { LoggerFactory.getLogger("UtilTest") }
                        single {
                            Json {
                                ignoreUnknownKeys = true
                                explicitNulls = false
                                isLenient = true
                            }
                        }
                        single { mockk<ApplicationConfig>(relaxed = true) }
                        single { mockk<ApplicationEnvironment>(relaxed = true) }
                        single { mockk<SongRepository>(relaxed = true) }
                        single { mockk<AWSSecretsManagerAsync>(relaxed = true) }
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

//    @Before
//    fun startKoinForTest() {
//        startKoin {
//            module {
//                 single { mockk<ApplicationEnvironment>() }
//            }
//        }
//    }

    @Test
    fun `test weaveRequest already exists`() =
        runTest {
            var actual: WeaveRequest
//        val mockEnvironment = Mockito.mock(ApplicationEnvironment::class.java)
//        Mockito.`when`(mockEnvironment.getSecureConfigString("arweave.walletJson")).thenReturn("")

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

            actual = Util.weaveRequest(song)

            val json =Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = true
            }

            val expected = WeaveRequest(
                json.encodeToString(
                    WeaveProps(
                        arweaveWalletJson = "",
                        files = emptyList(),
                        checkAndFund = false
                    )
                )
            )

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test weaveRequest doesn't exist`() =
        runTest {
            var actual: WeaveRequest
//        val mockEnvironment = Mockito.mock(ApplicationEnvironment::class.java)
//        Mockito.`when`(mockEnvironment.getSecureConfigString("arweave.walletJson")).thenReturn("")

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

            actual = Util.weaveRequest(song)

            val json =Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = true
            }

            val expected = WeaveRequest(
                json.encodeToString(
                    WeaveProps(
                        arweaveWalletJson = "",
                        files = emptyList<WeaveFile>().plus(WeaveFile("https://newm.io/agreement", "application/pdf")),
                        checkAndFund = false
                    )
                )
            )
            assertThat(actual).isEqualTo(expected)
        }
}
