package io.newm.server.features.user

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.config.database.ConfigEntity
import io.newm.server.config.database.ConfigTable
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EMAIL_WHITELIST
import io.newm.server.features.model.CountResponse
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserIdBody
import io.newm.shared.koin.inject
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.toHash
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            ConfigTable.deleteAll()
            UserTable.deleteAll()
            TwoFactorAuthTable.deleteAll()
        }
        val configRepository: ConfigRepository by inject()
        configRepository.invalidateCache()
    }

    @Test
    fun testPostUser() = runBlocking {
        val startTime = LocalDateTime.now()

        // Put 2FA code directly into database
        transaction {
            TwoFactorAuthEntity.new {
                email = testUser1.email!!
                codeHash = testUser1.authCode!!.toHash()
                expiresAt = LocalDateTime.now().plusSeconds(10)
            }
        }

        // Post User
        val response = client.post("v1/users") {
            contentType(ContentType.Application.Json)
            setBody(testUser1)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val userId = response.body<UserIdBody>().userId

        // Read User directly from database & verify it
        val (user, passwordHash) = transaction {
            UserEntity[userId].let { it.toModel() to it.passwordHash!! }
        }
        assertThat(user.createdAt).isAtLeast(startTime)
        assertThat(user.firstName).isEqualTo(testUser1.firstName)
        assertThat(user.lastName).isEqualTo(testUser1.lastName)
        assertThat(user.nickname).isEqualTo(testUser1.nickname)
        assertThat(user.pictureUrl).isEqualTo(testUser1.pictureUrl)
        assertThat(user.bannerUrl).isEqualTo(testUser1.bannerUrl)
        assertThat(user.websiteUrl).isEqualTo(testUser1.websiteUrl)
        assertThat(user.twitterUrl).isEqualTo(testUser1.twitterUrl)
        assertThat(user.instagramUrl).isEqualTo(testUser1.instagramUrl)
        assertThat(user.spotifyProfile).isEqualTo(testUser1.spotifyProfile)
        assertThat(user.soundCloudProfile).isEqualTo(testUser1.soundCloudProfile)
        assertThat(user.appleMusicProfile).isEqualTo(testUser1.appleMusicProfile)
        assertThat(user.location).isEqualTo(testUser1.location)
        assertThat(user.role).isEqualTo(testUser1.role)
        assertThat(user.genre).isEqualTo(testUser1.genre)
        assertThat(user.biography).isEqualTo(testUser1.biography)
        assertThat(user.walletAddress).isEqualTo(testUser1.walletAddress)
        assertThat(user.email).isEqualTo(testUser1.email)
        assertThat(testUser1.newPassword!!.verify(passwordHash)).isTrue()
        assertThat(user.isni).isEqualTo(testUser1.isni)
        assertThat(user.ipi).isEqualTo(testUser1.ipi)
        assertThat(user.companyName).isEqualTo(testUser1.companyName)
        assertThat(user.companyLogoUrl).isEqualTo(testUser1.companyLogoUrl)
        assertThat(user.companyIpRights).isEqualTo(testUser1.companyIpRights)
        assertThat(user.dspPlanSubscribed).isEqualTo(testUser1.dspPlanSubscribed)
    }

    @Test
    fun testPostUserWhitelistFailure() = runBlocking {
        // Put whitelist directly into database
        transaction {
            ConfigEntity.new(CONFIG_KEY_EMAIL_WHITELIST) {
                value = "^.*@newm\\.io$" // only allow newm.io emails
            }
        }

        // Post User
        val response = client.post("v1/users") {
            contentType(ContentType.Application.Json)
            setBody(testUser1)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun testPostUserWhitelistSuccess() = runBlocking {
        val startTime = LocalDateTime.now()

        // Put 2FA code directly into database
        transaction {
            TwoFactorAuthEntity.new {
                email = testUser1.email!!
                codeHash = testUser1.authCode!!.toHash()
                expiresAt = LocalDateTime.now().plusSeconds(10)
            }
        }

        // Put whitelist directly into database
        transaction {
            ConfigEntity.new(CONFIG_KEY_EMAIL_WHITELIST) {
                value = "^.*@projectnewm\\.io$" // only allow projectnewm.io emails
            }
        }

        // Post User
        val response = client.post("v1/users") {
            contentType(ContentType.Application.Json)
            setBody(testUser1)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val userId = response.body<UserIdBody>().userId

        // Read User directly from database & verify it
        val (user, passwordHash) = transaction {
            UserEntity[userId].let { it.toModel() to it.passwordHash!! }
        }
        assertThat(user.createdAt).isAtLeast(startTime)
        assertThat(user.firstName).isEqualTo(testUser1.firstName)
        assertThat(user.lastName).isEqualTo(testUser1.lastName)
        assertThat(user.nickname).isEqualTo(testUser1.nickname)
        assertThat(user.pictureUrl).isEqualTo(testUser1.pictureUrl)
        assertThat(user.bannerUrl).isEqualTo(testUser1.bannerUrl)
        assertThat(user.websiteUrl).isEqualTo(testUser1.websiteUrl)
        assertThat(user.twitterUrl).isEqualTo(testUser1.twitterUrl)
        assertThat(user.instagramUrl).isEqualTo(testUser1.instagramUrl)
        assertThat(user.spotifyProfile).isEqualTo(testUser1.spotifyProfile)
        assertThat(user.soundCloudProfile).isEqualTo(testUser1.soundCloudProfile)
        assertThat(user.appleMusicProfile).isEqualTo(testUser1.appleMusicProfile)
        assertThat(user.location).isEqualTo(testUser1.location)
        assertThat(user.role).isEqualTo(testUser1.role)
        assertThat(user.genre).isEqualTo(testUser1.genre)
        assertThat(user.biography).isEqualTo(testUser1.biography)
        assertThat(user.walletAddress).isEqualTo(testUser1.walletAddress)
        assertThat(user.email).isEqualTo(testUser1.email)
        assertThat(testUser1.newPassword!!.verify(passwordHash)).isTrue()
        assertThat(user.isni).isEqualTo(testUser1.isni)
        assertThat(user.ipi).isEqualTo(testUser1.ipi)
        assertThat(user.companyName).isEqualTo(testUser1.companyName)
        assertThat(user.companyLogoUrl).isEqualTo(testUser1.companyLogoUrl)
        assertThat(user.companyIpRights).isEqualTo(testUser1.companyIpRights)
        assertThat(user.dspPlanSubscribed).isEqualTo(testUser1.dspPlanSubscribed)
    }

    @Test
    fun testGetUser() = runBlocking {
        // Put User directly into database
        val userId = addUserToDatabase(testUser1)

        // Get User
        val response = client.get("v1/users/me") {
            bearerAuth(userId.toString())
            accept(ContentType.Application.Json)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        // verify it
        val user = response.body<User>()
        assertThat(user.id).isEqualTo(userId)
        assertThat(user.firstName).isEqualTo(testUser1.firstName)
        assertThat(user.lastName).isEqualTo(testUser1.lastName)
        assertThat(user.nickname).isEqualTo(testUser1.nickname)
        assertThat(user.pictureUrl).isEqualTo(testUser1.pictureUrl)
        assertThat(user.bannerUrl).isEqualTo(testUser1.bannerUrl)
        assertThat(user.websiteUrl).isEqualTo(testUser1.websiteUrl)
        assertThat(user.twitterUrl).isEqualTo(testUser1.twitterUrl)
        assertThat(user.instagramUrl).isEqualTo(testUser1.instagramUrl)
        assertThat(user.spotifyProfile).isEqualTo(testUser1.spotifyProfile)
        assertThat(user.soundCloudProfile).isEqualTo(testUser1.soundCloudProfile)
        assertThat(user.appleMusicProfile).isEqualTo(testUser1.appleMusicProfile)
        assertThat(user.location).isEqualTo(testUser1.location)
        assertThat(user.role).isEqualTo(testUser1.role)
        assertThat(user.genre).isEqualTo(testUser1.genre)
        assertThat(user.biography).isEqualTo(testUser1.biography)
        assertThat(user.walletAddress).isEqualTo(testUser1.walletAddress)
        assertThat(user.email).isEqualTo(testUser1.email)
        assertThat(user.isni).isEqualTo(testUser1.isni)
        assertThat(user.ipi).isEqualTo(testUser1.ipi)
        assertThat(user.companyName).isEqualTo(testUser1.companyName)
        assertThat(user.companyLogoUrl).isEqualTo(testUser1.companyLogoUrl)
        assertThat(user.companyIpRights).isEqualTo(testUser1.companyIpRights)
        assertThat(user.dspPlanSubscribed).isEqualTo(testUser1.dspPlanSubscribed)
    }

    @Test
    fun testPatchUser() = runBlocking {
        // Put User directly into database
        val userId = addUserToDatabase(testUser1)

        // Put 2FA code directly into database
        transaction {
            TwoFactorAuthEntity.new {
                email = testUser2.email!!
                codeHash = testUser2.authCode!!.toHash()
                expiresAt = LocalDateTime.now().plusSeconds(10)
            }
        }

        // Patch User1 with User2
        val response = client.patch("v1/users/me") {
            bearerAuth(userId.toString())
            contentType(ContentType.Application.Json)
            setBody(testUser2)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Read resulting User directly from database & verify it
        val (user, passwordHash) = transaction {
            UserEntity[userId].let { it.toModel() to it.passwordHash!! }
        }
        assertThat(user.id).isEqualTo(userId)
        assertThat(user.firstName).isEqualTo(testUser2.firstName)
        assertThat(user.lastName).isEqualTo(testUser2.lastName)
        assertThat(user.nickname).isEqualTo(testUser2.nickname)
        assertThat(user.pictureUrl).isEqualTo(testUser2.pictureUrl)
        assertThat(user.bannerUrl).isEqualTo(testUser2.bannerUrl)
        assertThat(user.websiteUrl).isEqualTo(testUser2.websiteUrl)
        assertThat(user.twitterUrl).isEqualTo(testUser2.twitterUrl)
        assertThat(user.instagramUrl).isEqualTo(testUser2.instagramUrl)
        assertThat(user.spotifyProfile).isEqualTo(testUser2.spotifyProfile)
        assertThat(user.soundCloudProfile).isEqualTo(testUser2.soundCloudProfile)
        assertThat(user.appleMusicProfile).isEqualTo(testUser2.appleMusicProfile)
        assertThat(user.location).isEqualTo(testUser2.location)
        assertThat(user.role).isEqualTo(testUser2.role)
        assertThat(user.genre).isEqualTo(testUser2.genre)
        assertThat(user.biography).isEqualTo(testUser2.biography)
        assertThat(user.walletAddress).isEqualTo(testUser2.walletAddress)
        assertThat(user.email).isEqualTo(testUser2.email)
        assertThat(testUser2.newPassword!!.verify(passwordHash)).isTrue()
        assertThat(user.isni).isEqualTo(testUser2.isni)
        assertThat(user.ipi).isEqualTo(testUser2.ipi)
        assertThat(user.companyName).isEqualTo(testUser2.companyName)
        assertThat(user.companyLogoUrl).isEqualTo(testUser2.companyLogoUrl)
        assertThat(user.companyIpRights).isEqualTo(testUser2.companyIpRights)
        assertThat(user.dspPlanSubscribed).isEqualTo(testUser2.dspPlanSubscribed)
    }

    @Test
    fun testDeleteUser() = runBlocking {
        // Put User directly into database
        val userId = addUserToDatabase(testUser1)

        // Get User
        val response = client.delete("v1/users/me") {
            bearerAuth(userId.toString())
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // verify that is gone directly form the database
        val exists = transaction { UserEntity.existsHavingId(userId) }
        assertThat(exists).isFalse()
    }

    @Test
    fun testPutUserPassword() = runBlocking {
        // Put User directly into database
        val userId = transaction {
            UserEntity.new {
                email = testUser1.email!!
                passwordHash = testUser1.newPassword!!.toHash()
            }
        }.id.value

        // Put 2FA code directly into database
        transaction {
            TwoFactorAuthEntity.new {
                email = testUser1.email!!
                codeHash = testUser1.authCode!!.toHash()
                expiresAt = LocalDateTime.now().plusSeconds(10)
            }
        }

        // Put new password
        val response = client.put("v1/users/password") {
            contentType(ContentType.Application.Json)
            setBody(
                User(
                    email = testUser1.email,
                    newPassword = testUser2.newPassword,
                    confirmPassword = testUser2.confirmPassword,
                    authCode = testUser1.authCode
                )
            )
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // verify it
        val passwordHash = transaction { UserEntity[userId].passwordHash!! }
        assertThat(testUser2.newPassword!!.verify(passwordHash)).isTrue()
    }

    @Test
    fun testGetAllUsers() = runBlocking {
        // Put Users directly into database
        val expectedUsers = mutableListOf<User>()
        for (offset in 0..30) {
            expectedUsers += addUserToDatabase(offset)
        }

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetAllUsersInDescendingOrder() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }
        val expectedUsers = allUsers.sortedByDescending { it.createdAt }

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("sortOrder", "desc")
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUsersByIds() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }

        // filter out 1st and last
        val expectedUsers = allUsers.subList(1, allUsers.size - 1)
        val ids = expectedUsers.map { it.id }.joinToString()

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("ids", ids)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUsersByRoles() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }

        // filter out 1st and last
        val expectedUsers = allUsers.subList(1, allUsers.size - 1)
        val roles = expectedUsers.map { it.role }.joinToString()

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("roles", roles)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUsersByGenres() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }

        // filter out 1st and last
        val expectedUsers = allUsers.subList(1, allUsers.size - 1)
        val genres = expectedUsers.map { it.genre }.joinToString()

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("genres", genres)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUsersByOlderThan() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }

        // filter out newest one
        val expectedUsers = allUsers.subList(0, allUsers.size - 1)
        val olderThan = allUsers.last().createdAt

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("olderThan", olderThan)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUsersByNewerThan() = runBlocking {
        // Put Users directly into database
        val allUsers = mutableListOf<User>()
        for (offset in 0..30) {
            allUsers += addUserToDatabase(offset)
        }

        // filter out oldest one
        val expectedUsers = allUsers.subList(1, allUsers.size)
        val newerThan = allUsers.first().createdAt

        // read back all Users forcing pagination
        var offset = 0
        val limit = 5
        val actualUsers = mutableListOf<User>()
        val token = expectedUsers.first().id.toString()
        while (true) {
            val response = client.get("v1/users") {
                bearerAuth(token)
                accept(ContentType.Application.Json)
                parameter("offset", offset)
                parameter("limit", limit)
                parameter("newerThan", newerThan)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val users = response.body<List<User>>()
            if (users.isEmpty()) break
            actualUsers += users
            offset += limit
        }

        // verify all
        assertThat(actualUsers.size).isEqualTo(expectedUsers.size)
        expectedUsers.forEachIndexed { i, expectedUser ->
            val actualUser = actualUsers[i]
            assertThat(actualUser.id).isEqualTo(expectedUser.id)
            assertThat(actualUser.firstName).isEqualTo(expectedUser.firstName)
            assertThat(actualUser.lastName).isEqualTo(expectedUser.lastName)
            assertThat(actualUser.nickname).isEqualTo(expectedUser.nickname)
            assertThat(actualUser.pictureUrl).isEqualTo(expectedUser.pictureUrl)
            assertThat(actualUser.bannerUrl).isEqualTo(expectedUser.bannerUrl)
            assertThat(actualUser.websiteUrl).isEqualTo(expectedUser.websiteUrl)
            assertThat(actualUser.twitterUrl).isEqualTo(expectedUser.twitterUrl)
            assertThat(actualUser.instagramUrl).isEqualTo(expectedUser.instagramUrl)
            assertThat(actualUser.spotifyProfile).isEqualTo(expectedUser.spotifyProfile)
            assertThat(actualUser.soundCloudProfile).isEqualTo(expectedUser.soundCloudProfile)
            assertThat(actualUser.appleMusicProfile).isEqualTo(expectedUser.appleMusicProfile)
            assertThat(actualUser.location).isEqualTo(expectedUser.location)
            assertThat(actualUser.role).isEqualTo(expectedUser.role)
            assertThat(actualUser.genre).isEqualTo(expectedUser.genre)
            assertThat(actualUser.biography).isEqualTo(expectedUser.biography)
            assertThat(actualUser.isni).isEqualTo(expectedUser.isni)
            assertThat(actualUser.ipi).isEqualTo(expectedUser.ipi)
            assertThat(actualUser.companyName).isEqualTo(expectedUser.companyName)
            assertThat(actualUser.companyLogoUrl).isEqualTo(expectedUser.companyLogoUrl)
            assertThat(actualUser.companyIpRights).isEqualTo(expectedUser.companyIpRights)
            assertThat(actualUser.dspPlanSubscribed).isEqualTo(expectedUser.dspPlanSubscribed)
        }
    }

    @Test
    fun testGetUserCount() = runBlocking {
        var count = 1L
        while (true) {
            val response = client.get("v1/users/count") {
                bearerAuth(testUserToken)
            }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualCount = response.body<CountResponse>().count
            assertThat(actualCount).isEqualTo(count)

            if (++count == 11L) break

            transaction {
                UserEntity.new {
                    this.email = "user$count@newm.io"
                }
            }
        }
    }
}

private fun addUserToDatabase(offset: Int): User = transaction {
    UserEntity.new {
        firstName = "firstName$offset"
        lastName = "lastName$offset"
        nickname = "nickname$offset"
        pictureUrl = "https://picture/$offset"
        bannerUrl = "https://banner/$offset"
        websiteUrl = "https://website/$offset"
        twitterUrl = "https://twitter/$offset"
        instagramUrl = "https://instagram/$offset"
        spotifyProfile = "spotifyProfile$offset"
        soundCloudProfile = "soundCloudProfile$offset"
        appleMusicProfile = "appleMusicProfile$offset"
        location = "location$offset"
        role = "role$offset"
        genre = "genre$offset"
        biography = "biography$offset"
        email = "email$offset"
        isni = "isni$offset"
        ipi = "ipi$offset"
        companyName = "companyName$offset"
        companyLogoUrl = "https://companylogo/$offset"
        companyIpRights = offset % 2 == 0
        dspPlanSubscribed = offset % 2 == 0
    }
}.toModel(includeAll = false)
