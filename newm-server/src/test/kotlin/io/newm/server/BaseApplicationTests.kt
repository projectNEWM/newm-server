package io.newm.server

import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.config.database.ConfigEntity
import io.newm.server.config.database.ConfigTable
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.database.CollaborationTable
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.earnings.database.EarningEntity
import io.newm.server.features.earnings.database.EarningsTable
import io.newm.server.features.marketplace.database.MarketplaceBookmarkTable
import io.newm.server.features.marketplace.database.MarketplacePurchaseTable
import io.newm.server.features.marketplace.database.MarketplaceSaleTable
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.referralhero.repo.ReferralHeroRepository
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.ReleaseTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongReceiptTable
import io.newm.server.features.song.database.SongSmartLinkTable
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.ReleaseType
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
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeTable
import io.newm.server.features.walletconnection.database.WalletConnectionTable
import io.newm.server.ktx.asValidUrl
import io.newm.server.model.ClientPlatform
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.server.typealiases.SongId
import io.newm.shared.auth.Password
import io.newm.shared.serialization.BigDecimalSerializer
import io.newm.shared.serialization.BigIntegerSerializer
import io.newm.shared.serialization.LocalDateSerializer
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.coroutines.runBlocking
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
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class BaseApplicationTests {
    protected val application = TestApplication {
        environment {
            config = ApplicationConfig("test-application.conf")
        }
    }

    protected val client: HttpClient by lazy {
        application
            .createClient {
                install(ContentNegotiation) {
                    json(
                        json = Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            isLenient = true
                            serializersModule = SerializersModule {
                                contextual(BigDecimal::class, BigDecimalSerializer)
                                contextual(BigInteger::class, BigIntegerSerializer)
                                contextual(UUID::class, UUIDSerializer)
                                contextual(LocalDateTime::class, LocalDateTimeSerializer)
                                contextual(LocalDate::class, LocalDateSerializer)
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

    private lateinit var hikariDataSource: HikariDataSource

    @BeforeAll
    fun beforeAll() {
        hikariDataSource = HikariDataSource().apply {
            driverClassName = TestContext.container.driverClassName
            jdbcUrl = TestContext.container.jdbcUrl
            username = TestContext.container.username
            password = TestContext.container.password
        }
        Database.connect(hikariDataSource)
        transaction {
            SchemaUtils.create(
                ConfigTable,
                UserTable,
                TwoFactorAuthTable,
                KeyTable,
                SongTable,
                SongSmartLinkTable,
                CollaborationTable,
                PlaylistTable,
                SongsInPlaylistsTable,
                SongReceiptTable,
                WalletConnectionChallengeTable,
                WalletConnectionTable,
                MarketplaceBookmarkTable,
                MarketplaceSaleTable,
                MarketplacePurchaseTable,
                EarningsTable
            )
            val tables = listOf(
                ConfigTable,
                UserTable,
                TwoFactorAuthTable,
                KeyTable,
                SongTable,
                SongSmartLinkTable,
                CollaborationTable,
                PlaylistTable,
                SongsInPlaylistsTable,
                SongReceiptTable,
                WalletConnectionChallengeTable,
                WalletConnectionTable,
                MarketplaceBookmarkTable,
                MarketplaceSaleTable,
                MarketplacePurchaseTable,
                EarningsTable
            )
            exec("TRUNCATE ${tables.joinToString(", ") { it.tableName }} RESTART IDENTITY CASCADE")
        }
        runBlocking {
            application.start()
        }
        loadKoinModules(
            module {
                single { mockk<GoogleUserProvider>(relaxed = true) }
                single { mockk<FacebookUserProvider>(relaxed = true) }
                single { mockk<LinkedInUserProvider>(relaxed = true) }
                single { mockk<AppleUserProvider>(relaxed = true) }
                single { mockk<S3Client>(relaxed = true) }
                single { mockk<S3Presigner>(relaxed = true) }
                single { mockk<KmsAsyncClient>(relaxed = true) }
                single { mockk<RecaptchaRepository>(relaxed = true) }
                single { mockk<ReferralHeroRepository>(relaxed = true) }
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
        runBlocking {
            application.stop()
        }
        stopKoin()
        hikariDataSource.close()
    }

    protected fun addUserToDatabase(user: User): UUID =
        transaction {
            UserEntity.new {
                signupPlatform = user.signupPlatform ?: ClientPlatform.Studio
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
            CollaborationEntity
                .new {
                    email = collab.email!!
                    songId = EntityID(collab.songId!!, SongTable)
                    roles = collab.roles!!
                    credited = collab.credited!!
                    featured = collab.featured ?: false
                    royaltyRate = collab.royaltyRate?.toFloat()
                    status = CollaborationStatus.Accepted
                }.id.value
        }

    protected fun addSongToDatabase(
        release: Release,
        song: Song
    ): UUID =
        transaction {
            val releaseId = ReleaseEntity
                .new {
                    archived = false
                    this.ownerId = EntityID(release.ownerId!!, UserTable)
                    this.title = release.title!!
                    // TODO: Refactor 'single' hardcoding once we have album support in the UI/UX
                    releaseType = ReleaseType.SINGLE
                    barcodeType = release.barcodeType
                    barcodeNumber = release.barcodeNumber
                    releaseDate = release.releaseDate
                    publicationDate = release.publicationDate
                    coverArtUrl = release.coverArtUrl?.asValidUrl()
                    arweaveCoverArtUrl = release.arweaveCoverArtUrl
                    hasSubmittedForDistribution = false
                    errorMessage = song.errorMessage
                    forceDistributed = false
                }.id.value
            SongEntity
                .new {
                    ownerId = EntityID(song.ownerId!!, UserTable)
                    title = song.title!!
                    genres = song.genres!!
                    moods = song.moods
                    description = song.description
                    this.releaseId = EntityID(releaseId, ReleaseTable)
                    track = song.track
                    language = song.language
                    compositionCopyrightOwner = song.compositionCopyrightOwner
                    compositionCopyrightYear = song.compositionCopyrightYear
                    phonographicCopyrightOwner = song.phonographicCopyrightOwner
                    phonographicCopyrightYear = song.phonographicCopyrightYear
                    parentalAdvisory = song.parentalAdvisory
                    isrc = song.isrc
                    iswc = song.iswc
                    ipis = song.ipis
                    lyricsUrl = song.lyricsUrl?.asValidUrl()
                    arweaveClipUrl = song.arweaveClipUrl
                    arweaveLyricsUrl = song.arweaveLyricsUrl
                    arweaveTokenAgreementUrl = song.arweaveTokenAgreementUrl
                    duration = song.duration
                    originalAudioUrl = song.originalAudioUrl
                }.id.value
        }

    protected fun addEarningsToDatabase(
        songId: SongId,
        stakeAddress: String,
        amount: Long,
        createdAt: LocalDateTime
    ): UUID =
        transaction {
            EarningEntity.new {
                this.songId = EntityID(songId, SongTable)
                this.amount = amount
                this.stakeAddress = stakeAddress
                this.memo = "default"
                this.createdAt = createdAt
            }
        }.id.value

    protected fun addConfigToDatabase(
        id: String,
        value: String
    ) = transaction {
        ConfigEntity.new(id) {
            this.value = value
        }
    }
}
