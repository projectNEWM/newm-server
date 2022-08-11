package io.newm.server.features.user

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.auth.twofactor.database.TwoFactorAuthEntity
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.ext.existsHavingId
import io.newm.server.ext.toHash
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.User
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        val emails = testUsers.map { it.email!! }
        transaction {
            UserTable.deleteWhere { UserTable.email inList emails }
            TwoFactorAuthTable.deleteWhere { TwoFactorAuthTable.email inList emails }
        }
    }

    @Test
    fun testPutUser() = runBlocking {
        // Put 2FA code directly into database
        transaction {
            TwoFactorAuthEntity.new {
                email = testUser1.email!!
                codeHash = testUser1.authCode!!.toHash()
                expiresAt = LocalDateTime.now().plusSeconds(10)
            }
        }

        // Put User
        val response = client.put("v1/users") {
            contentType(ContentType.Application.Json)
            setBody(testUser1)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        // Read User directly from database & verify it
        val (user, passwordHash) = transaction {
            UserEntity.getByEmail(testUser1.email!!)!!.let { it.toModel() to it.passwordHash!! }
        }
        assertThat(user.firstName).isEqualTo(testUser1.firstName)
        assertThat(user.lastName).isEqualTo(testUser1.lastName)
        assertThat(user.nickname).isEqualTo(testUser1.nickname)
        assertThat(user.pictureUrl).isEqualTo(testUser1.pictureUrl)
        assertThat(user.role).isEqualTo(testUser1.role)
        assertThat(user.genre).isEqualTo(testUser1.genre)
        assertThat(user.email).isEqualTo(testUser1.email)
        assertThat(testUser1.newPassword!!.verify(passwordHash)).isTrue()
    }

    @Test
    fun testGetUser() = runBlocking {
        // Put User directly into database
        val userId = transaction {
            UserEntity.new {
                firstName = testUser1.firstName
                lastName = testUser1.lastName
                nickname = testUser1.nickname
                pictureUrl = testUser1.pictureUrl
                role = testUser1.role
                genre = testUser1.genre
                email = testUser1.email!!
            }
        }.id.value

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
        assertThat(user.role).isEqualTo(testUser1.role)
        assertThat(user.genre).isEqualTo(testUser1.genre)
        assertThat(user.email).isEqualTo(testUser1.email)
    }

    @Test
    fun testPatchUser() = runBlocking {
        // Put User directly into database
        val userId = transaction {
            UserEntity.new {
                firstName = testUser1.firstName
                lastName = testUser1.lastName
                nickname = testUser1.nickname
                pictureUrl = testUser1.pictureUrl
                role = testUser1.role
                genre = testUser1.genre
                email = testUser1.email!!
                passwordHash = testUser1.newPassword!!.toHash()
            }
        }.id.value

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
        assertThat(user.role).isEqualTo(testUser2.role)
        assertThat(user.genre).isEqualTo(testUser2.genre)
        assertThat(user.email).isEqualTo(testUser2.email)
        assertThat(testUser2.newPassword!!.verify(passwordHash)).isTrue()
    }

    @Test
    fun testDeleteUser() = runBlocking {
        // Put User directly into database
        val userId = transaction {
            UserEntity.new {
                firstName = testUser1.firstName
                lastName = testUser1.lastName
                nickname = testUser1.nickname
                pictureUrl = testUser1.pictureUrl
                role = testUser1.role
                genre = testUser1.genre
                email = testUser1.email!!
            }
        }.id.value

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
}
