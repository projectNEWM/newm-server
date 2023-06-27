package io.newm.server

import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.TestApplication
import io.mockk.mockk
import io.newm.server.auth.jwt.database.JwtTable
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.config.database.ConfigTable
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.database.CollaborationTable
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.server.features.user.oauth.providers.AppleUserProvider
import io.newm.server.features.user.oauth.providers.FacebookUserProvider
import io.newm.server.features.user.oauth.providers.GoogleUserProvider
import io.newm.server.features.user.oauth.providers.LinkedInUserProvider
import io.newm.server.ktx.asValidUrl
import io.newm.shared.auth.Password
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class BaseApplicationTests {

    protected val application = TestApplication {
        environment {
            config = ApplicationConfig("test-application.conf")
        }
    }

    protected val client: HttpClient by lazy {
        application.createClient {
            install(ContentNegotiation) {
                json()
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
            )
        }
        application.start()
        loadKoinModules(
            module {
                single { mockk<CardanoRepository>(relaxed = true) }
                single { mockk<GoogleUserProvider>(relaxed = true) }
                single { mockk<FacebookUserProvider>(relaxed = true) }
                single { mockk<LinkedInUserProvider>(relaxed = true) }
                single { mockk<AppleUserProvider>(relaxed = true) }
            }
        )
    }

    @AfterAll
    fun afterAll() {
        application.stop()
    }

    protected fun addUserToDatabase(user: User): UUID = transaction {
        UserEntity.new {
            firstName = user.firstName
            lastName = user.lastName
            nickname = user.nickname
            pictureUrl = user.pictureUrl
            bannerUrl = user.bannerUrl
            websiteUrl = user.websiteUrl
            twitterUrl = user.twitterUrl
            instagramUrl = user.instagramUrl
            location = user.location
            role = user.role
            genre = user.genre
            biography = user.biography
            walletAddress = user.walletAddress
            email = user.email!!
            passwordHash = (user.newPassword ?: Password("dummyPassword")).toHash()
            companyName = user.companyName
            companyLogoUrl = user.companyLogoUrl
            companyIpRights = user.companyIpRights
        }
    }.id.value

    protected fun addCollabToDatabase(collab: Collaboration): UUID = transaction {
        CollaborationEntity.new {
            email = collab.email!!
            songId = EntityID(collab.songId!!, SongTable)
            role = collab.role
            credited = collab.credited!!
            royaltyRate = collab.royaltyRate
            status = CollaborationStatus.Accepted
        }.id.value
    }

    protected fun addSongToDatabase(song: Song): UUID = transaction {
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
            copyright = song.copyright
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
        }.id.value
    }

    companion object {
        @Container
        val container = PostgreSQLContainer<Nothing>("postgres:12").apply {
            withDatabaseName("newm-db")
            withUsername("tester")
            withPassword("newm1234")
        }
    }
}
