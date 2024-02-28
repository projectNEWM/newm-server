package io.newm.server

import com.amazonaws.services.s3.AmazonS3
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import io.newm.server.auth.jwt.database.JwtTable
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.config.database.ConfigEntity
import io.newm.server.config.database.ConfigTable
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.database.CollaborationTable
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongReceiptTable
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.QUALIFIER_APPLE_MUSIC_PROFILE_URL_VERIFIER
import io.newm.server.features.user.QUALIFIER_SOUND_CLOUD_PROFILE_URL_VERIFIER
import io.newm.server.features.user.QUALIFIER_SPOTIFY_PROFILE_URL_VERIFIER
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.server.features.user.oauth.providers.AppleUserProvider
import io.newm.server.features.user.oauth.providers.FacebookUserProvider
import io.newm.server.features.user.oauth.providers.GoogleUserProvider
import io.newm.server.features.user.oauth.providers.LinkedInUserProvider
import io.newm.server.features.user.verify.OutletProfileUrlVerifier
import io.newm.server.ktx.asValidUrl
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.auth.Password
import io.newm.shared.serialization.BigDecimalSerializer
import io.newm.shared.serialization.BigIntegerSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class BaseApplicationTests {
    protected val application =
        TestApplication {
            environment {
                config = ApplicationConfig("test-application.conf")
            }
        }

    protected val client: HttpClient by lazy {
        application.createClient {
            install(ContentNegotiation) {
                json(
                    json =
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            isLenient = true
                            serializersModule =
                                SerializersModule {
                                    contextual(BigDecimal::class, BigDecimalSerializer)
                                    contextual(BigInteger::class, BigIntegerSerializer)
                                }
                        }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                socketTimeoutMillis = 30.seconds.inWholeMilliseconds
            }
        }.also { client ->
            loadKoinModules(
                module {
                    single { client }
                }
            )
        }
    }

    protected val testUserEmail: String = "tester@projectnewm.io"

    protected val testUserId: UUID by lazy {
        transaction {
            UserEntity.new {
                email = testUserEmail
                firstName = "Tester"
                lastName = "Newm"
            }
        }.id.value
    }

    protected val testUserToken: String
        get() = testUserId.toString()

    @BeforeAll
    fun beforeAll() {
        Database.connect(
            HikariDataSource().apply {
                driverClassName = container.driverClassName
                jdbcUrl = container.jdbcUrl
                username = container.username
                password = container.password
            }
        )
        transaction {
            SchemaUtils.create(
                ConfigTable,
                UserTable,
                TwoFactorAuthTable,
                JwtTable,
                KeyTable,
                SongTable,
                CollaborationTable,
                PlaylistTable,
                SongsInPlaylistsTable,
                SongReceiptTable,
            )
        }
        application.start()
        loadKoinModules(
            module {
                single { mockk<GoogleUserProvider>(relaxed = true) }
                single { mockk<FacebookUserProvider>(relaxed = true) }
                single { mockk<LinkedInUserProvider>(relaxed = true) }
                single { mockk<AppleUserProvider>(relaxed = true) }
                single { mockk<AmazonS3>(relaxed = true) }
                single { mockk<RecaptchaRepository>(relaxed = true) }
                single<OutletProfileUrlVerifier>(QUALIFIER_SPOTIFY_PROFILE_URL_VERIFIER) {
                    mockk<OutletProfileUrlVerifier>(relaxed = true)
                }
                single<OutletProfileUrlVerifier>(QUALIFIER_APPLE_MUSIC_PROFILE_URL_VERIFIER) {
                    mockk<OutletProfileUrlVerifier>(relaxed = true)
                }
                single<OutletProfileUrlVerifier>(QUALIFIER_SOUND_CLOUD_PROFILE_URL_VERIFIER) {
                    mockk<OutletProfileUrlVerifier>(relaxed = true)
                }
            }
        )
    }

    @AfterAll
    fun afterAll() {
        application.stop()
        stopKoin()
    }

    protected fun addUserToDatabase(user: User): UUID =
        transaction {
            UserEntity.new {
                firstName = user.firstName
                lastName = user.lastName
                nickname = user.nickname
                pictureUrl = user.pictureUrl
                bannerUrl = user.bannerUrl
                websiteUrl = user.websiteUrl
                twitterUrl = user.twitterUrl
                instagramUrl = user.instagramUrl
                spotifyProfile = user.spotifyProfile
                soundCloudProfile = user.soundCloudProfile
                appleMusicProfile = user.appleMusicProfile
                location = user.location
                role = user.role
                genre = user.genre
                biography = user.biography
                walletAddress = user.walletAddress
                email = user.email!!
                passwordHash = (user.newPassword ?: Password("dummyPassword")).toHash()
                isni = user.isni
                ipi = user.ipi
                companyName = user.companyName
                companyLogoUrl = user.companyLogoUrl
                companyIpRights = user.companyIpRights
            }
        }.id.value

    protected fun addCollabToDatabase(collab: Collaboration): UUID =
        transaction {
            CollaborationEntity.new {
                email = collab.email!!
                songId = EntityID(collab.songId!!, SongTable)
                role = collab.role
                credited = collab.credited!!
                royaltyRate = collab.royaltyRate?.toFloat()
                status = CollaborationStatus.Accepted
            }.id.value
        }

    protected fun addSongToDatabase(song: Song): UUID =
        transaction {
            SongEntity.new {
                ownerId = EntityID(song.ownerId!!, UserTable)
                title = song.title!!
                genres = song.genres!!.toTypedArray()
                moods = song.moods?.toTypedArray()
                coverArtUrl = song.coverArtUrl?.asValidUrl()
                description = song.description
                album = song.album
                track = song.track
                language = song.language
                compositionCopyrightOwner = song.compositionCopyrightOwner
                compositionCopyrightYear = song.compositionCopyrightYear
                phonographicCopyrightOwner = song.phonographicCopyrightOwner
                phonographicCopyrightYear = song.phonographicCopyrightYear
                parentalAdvisory = song.parentalAdvisory
                isrc = song.isrc
                iswc = song.iswc
                ipis = song.ipis?.toTypedArray()
                releaseDate = song.releaseDate
                lyricsUrl = song.lyricsUrl?.asValidUrl()
                arweaveClipUrl = song.arweaveClipUrl
                arweaveCoverArtUrl = song.arweaveCoverArtUrl
                arweaveLyricsUrl = song.arweaveLyricsUrl
                arweaveTokenAgreementUrl = song.arweaveTokenAgreementUrl
                releaseDate = song.releaseDate
                publicationDate = song.publicationDate
                duration = song.duration
                originalAudioUrl = song.originalAudioUrl
            }.id.value
        }

    protected fun addConfigToDatabase(
        id: String,
        value: String
    ) = transaction {
        ConfigEntity.new(id) {
            this.value = value
        }
    }

    companion object {
        @Container
        val container =
            PostgreSQLContainer<Nothing>("postgres:12").apply {
                withDatabaseName("newm-db")
                withUsername("tester")
                withPassword("newm1234")
            }
    }
}
