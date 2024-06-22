package io.newm.server.features.collaborations

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
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.database.CollaborationTable
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.server.features.collaboration.model.CollaborationIdBody
import io.newm.server.features.collaboration.model.CollaborationReply
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.model.Collaborator
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.existsHavingId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CollaborationRoutesTests : BaseApplicationTests() {
    @BeforeEach
    fun beforeEach() {
        transaction {
            CollaborationTable.deleteAll()
            SongTable.deleteAll()
            UserTable.deleteWhere { id neq testUserId }
        }
    }

    @Test
    fun testPostCollaboration() =
        runBlocking {
            val startTime = LocalDateTime.now()

            // Add Song directly into database
            val songId =
                transaction {
                    SongEntity
                        .new {
                            ownerId = EntityID(testUserId, UserTable)
                            title = "Song"
                            genres = arrayOf("Genre")
                        }.id.value
                }

            val expectedCollaboration =
                Collaboration(
                    songId = songId,
                    email = "collaborator@email.com",
                    role = "Role",
                    royaltyRate = 0.5f.toBigDecimal(),
                    credited = true,
                    featured = true
                )

            // Post
            val response =
                client.post("v1/collaborations") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(expectedCollaboration)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val collaborationId = response.body<CollaborationIdBody>().collaborationId

            // Read Collaboration directly from database & verify it
            val actualCollaboration = transaction { CollaborationEntity[collaborationId] }.toModel()
            assertThat(actualCollaboration.createdAt).isAtLeast(startTime)
            assertThat(actualCollaboration.songId).isEqualTo(expectedCollaboration.songId)
            assertThat(actualCollaboration.email).isEqualTo(expectedCollaboration.email)
            assertThat(actualCollaboration.role).isEqualTo(expectedCollaboration.role)
            assertThat(actualCollaboration.royaltyRate).isEqualTo(expectedCollaboration.royaltyRate)
            assertThat(actualCollaboration.credited).isEqualTo(expectedCollaboration.credited)
            assertThat(actualCollaboration.featured).isEqualTo(expectedCollaboration.featured)
            assertThat(actualCollaboration.status).isEqualTo(CollaborationStatus.Editing)
        }

    @Test
    fun testPatchCollaboration() =
        runBlocking {
            // Add Collaboration directly into database
            val collaboration1 = addCollaborationToDatabase(testUserId)

            val collaboration2 =
                Collaboration(
                    email = "collaborator2@email.com",
                    role = "Role2",
                    royaltyRate = 2.toBigDecimal() * collaboration1.royaltyRate!!,
                    credited = !collaboration1.credited!!,
                    featured = !collaboration1.featured!!,
                )

            // Patch collaboration1 with collaboration2
            val response =
                client.patch("v1/collaborations/${collaboration1.id}") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(collaboration2)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // Read Collaboration directly from database & verify it
            val collaboration = transaction { CollaborationEntity[collaboration1.id!!] }.toModel()
            assertThat(collaboration.id).isEqualTo(collaboration1.id)
            assertThat(collaboration.createdAt).isEqualTo(collaboration1.createdAt)
            assertThat(collaboration.songId).isEqualTo(collaboration1.songId)
            assertThat(collaboration.email).isEqualTo(collaboration2.email)
            assertThat(collaboration.role).isEqualTo(collaboration2.role)
            assertThat(collaboration.royaltyRate).isEqualTo(collaboration2.royaltyRate)
            assertThat(collaboration.credited).isEqualTo(collaboration2.credited)
            assertThat(collaboration.featured).isEqualTo(collaboration2.featured)
            assertThat(collaboration.status).isEqualTo(CollaborationStatus.Editing)
        }

    @Test
    fun testDeleteCollaboration() =
        runBlocking {
            // Add Collaboration directly into database
            val collaborationId = addCollaborationToDatabase(testUserId).id!!

            // delete it
            val response =
                client.delete("v1/collaborations/$collaborationId") {
                    bearerAuth(testUserToken)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // make sure doesn't exist in database
            val exists = transaction { CollaborationEntity.existsHavingId(collaborationId) }
            assertThat(exists).isFalse()
        }

    @Test
    fun testGetCollaboration() =
        runBlocking {
            // Add Collaboration directly into database
            val expectedCollaboration = addCollaborationToDatabase(testUserId)

            // Get it
            val response =
                client.get("v1/collaborations/${expectedCollaboration.id}") {
                    bearerAuth(testUserToken)
                    accept(ContentType.Application.Json)
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val actualCollaboration = response.body<Collaboration>()
            assertThat(actualCollaboration).isEqualTo(expectedCollaboration)
        }

    @Test
    fun testGetAllCollaborations() =
        runBlocking {
            // Add Collaborations directly into database
            val expectedCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                expectedCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetAllCollaborationsInDescendingOrder() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }
            val expectedCollaborations = allCollaborations.sortedByDescending { it.createdAt }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("sortOrder", "desc")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testCollaborationCount() =
        runBlocking {
            var count = 0L
            while (true) {
                val response =
                    client.get("v1/collaborations/count") {
                        bearerAuth(testUserToken)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val actualCount = response.body<CountResponse>().count
                assertThat(actualCount).isEqualTo(count)

                if (++count == 10L) break

                // Add collaborations directly into database
                addCollaborationToDatabase(testUserId, count.toInt())
            }
        }

    @Test
    fun testGetCollaborationsByInbound() =
        runBlocking {
            // Create collaboration invitations send from others to testUser1
            val expectedCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                expectedCollaborations += addCollaborationToDatabase(offset = offset, email = testUserEmail)
            }

            // Create collaboration invitations send from testUser1 to others - these should be filtered out
            for (offset in 31..40) {
                addCollaborationToDatabase(ownerId = testUserId, offset = offset)
            }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("inbound", true)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByIds() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val ids = expectedCollaborations.joinToString { it.id.toString() }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByIdsExclusion() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val ids = allCollaborations.filter { it !in expectedCollaborations }.joinToString { "-${it.id}" }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsBySongIds() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val songIds = expectedCollaborations.joinToString { it.songId.toString() }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsBySongIdsExclusion() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val songIds = allCollaborations.filter { it !in expectedCollaborations }.joinToString { "-${it.songId}" }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByEmails() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val emails = expectedCollaborations.joinToString { it.email!! }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("emails", emails)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByEmailsExclusion() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out 1st and last
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size - 1)
            val emails = allCollaborations.filter { it !in expectedCollaborations }.joinToString { "-${it.email}" }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("emails", emails)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByStatuses() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            for (expectedStatus in CollaborationStatus.entries) {
                // filter out
                val expectedCollaborations = allCollaborations.filter { it.status == expectedStatus }

                // Get all collaborations forcing pagination
                var offset = 0
                val limit = 5
                val actualCollaborations = mutableListOf<Collaboration>()
                while (true) {
                    val response =
                        client.get("v1/collaborations") {
                            bearerAuth(testUserToken)
                            accept(ContentType.Application.Json)
                            parameter("offset", offset)
                            parameter("limit", limit)
                            parameter("statuses", expectedStatus)
                        }
                    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                    val collaborations = response.body<List<Collaboration>>()
                    if (collaborations.isEmpty()) break
                    actualCollaborations += collaborations
                    offset += limit
                }

                // verify all
                assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
            }
        }

    @Test
    fun testGetCollaborationsByStatusesExclusion() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            for (expectedStatus in CollaborationStatus.entries) {
                // filter out
                val expectedCollaborations = allCollaborations.filter { it.status != expectedStatus }

                // Get all collaborations forcing pagination
                var offset = 0
                val limit = 5
                val actualCollaborations = mutableListOf<Collaboration>()
                while (true) {
                    val response =
                        client.get("v1/collaborations") {
                            bearerAuth(testUserToken)
                            accept(ContentType.Application.Json)
                            parameter("offset", offset)
                            parameter("limit", limit)
                            parameter("statuses", "-$expectedStatus")
                        }
                    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                    val collaborations = response.body<List<Collaboration>>()
                    if (collaborations.isEmpty()) break
                    actualCollaborations += collaborations
                    offset += limit
                }

                // verify all
                assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
            }
        }

    @Test
    fun testGetCollaborationsByOlderThan() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out newest one
            val expectedCollaborations = allCollaborations.subList(0, allCollaborations.size - 1)
            val olderThan = allCollaborations.last().createdAt

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("olderThan", olderThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetCollaborationsByNewerThan() =
        runBlocking {
            // Add collaborations directly into database
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                allCollaborations += addCollaborationToDatabase(testUserId, offset)
            }

            // filter out oldest one
            val expectedCollaborations = allCollaborations.subList(1, allCollaborations.size)
            val newerThan = allCollaborations.first().createdAt

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborations = mutableListOf<Collaboration>()
            while (true) {
                val response =
                    client.get("v1/collaborations") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("newerThan", newerThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborations = response.body<List<Collaboration>>()
                if (collaborations.isEmpty()) break
                actualCollaborations += collaborations
                offset += limit
            }

            // verify all
            assertThat(actualCollaborations).isEqualTo(expectedCollaborations)
        }

    @Test
    fun testGetAllCollaborators() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }
            val expectedCollaborators = allCollaborators.sortedBy { it.email }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            assertThat(actualCollaborators).isEqualTo(expectedCollaborators)
        }

    @Test
    fun testGetAllCollaboratorsInDescendingOrder() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }
            val expectedCollaborators = allCollaborators.sortedByDescending { it.email }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("sortOrder", "desc")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            assertThat(actualCollaborators).isEqualTo(expectedCollaborators)
        }

    @Test
    fun testGetCollaboratorsByExcludeMe() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = testUserEmail.takeIf { offset % 3 == 0 } ?: "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }

                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out test user
            val expectedCollaborators = allCollaborators.filter { it.email != testUserEmail }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("excludeMe", true)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testGetCollaboratorsBySongId() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out 1st and last
            val expectedCollaborators = allCollaborators.subList(1, allCollaborators.size - 1)
            val songIds =
                allCollaborations
                    .filter {
                        it.email != allCollaborators.first().email && it.email != allCollaborators.last().email
                    }.joinToString { it.songId.toString() }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testGetCollaboratorsBySongIdExclusion() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out 1st and last
            val expectedCollaborators = allCollaborators.subList(1, allCollaborators.size - 1)
            val songIds =
                allCollaborations
                    .filterNot {
                        it.email != allCollaborators.first().email && it.email != allCollaborators.last().email
                    }.joinToString { "-${it.songId}" }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testGetCollaboratorsByEmails() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out 1st and last
            val expectedCollaborators = allCollaborators.subList(1, allCollaborators.size - 1)
            val emails =
                allCollaborations
                    .filter {
                        it.email != allCollaborators.first().email && it.email != allCollaborators.last().email
                    }.joinToString { it.email!! }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("emails", emails)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testGetCollaboratorsByEmailsExclusion() =
        runBlocking {
            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = "collaborator$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out 1st and last
            val expectedCollaborators = allCollaborators.subList(1, allCollaborators.size - 1)
            val emails =
                allCollaborations
                    .filterNot {
                        it.email != allCollaborators.first().email && it.email != allCollaborators.last().email
                    }.joinToString { "-${it.email}" }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("emails", emails)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testGetCollaboratorsByPhrase() =
        runBlocking {
            val phrase = "abcde"

            fun phraseOrBlank(
                offset: Int,
                target: Int
            ) = phrase.takeIf { offset % 3 == target }.orEmpty()

            // Add Collaborations directly into database
            val allCollaborators = mutableListOf<Collaborator>()
            val allCollaborations = mutableListOf<Collaboration>()
            for (offset in 0..30) {
                val email = "collaborator${phraseOrBlank(offset, 0)}$offset@email.com"
                val user =
                    takeIf { offset % 2 == 0 }?.let {
                        transaction {
                            UserEntity.new {
                                this.email = email
                            }
                        }.toModel(false)
                    }
                val songCount = (offset % 4) + 1
                val status = user?.let { CollaborationStatus.Accepted }
                for (i in 1..songCount) {
                    allCollaborations += addCollaborationToDatabase(testUserId, i, email, status)
                }
                allCollaborators += Collaborator(email, songCount.toLong(), user)
            }

            // filter out for phrase
            val expectedCollaborators =
                allCollaborators.filter { collab ->
                    collab.email!!.contains(phrase) ||
                        collab.user?.firstName?.contains(phrase) == true ||
                        collab.user?.lastName?.contains(phrase) == true
                }

            // Get all collaborations forcing pagination
            var offset = 0
            val limit = 5
            val actualCollaborators = mutableListOf<Collaborator>()
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators") {
                        bearerAuth(testUserToken)
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("phrase", phrase)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val collaborators = response.body<List<Collaborator>>()
                if (collaborators.isEmpty()) break
                actualCollaborators += collaborators
                offset += limit
            }

            // verify all
            val actualSorted = actualCollaborators.sortedBy { it.email }
            val expectedSorted = expectedCollaborators.sortedBy { it.email }
            assertThat(actualSorted).isEqualTo(expectedSorted)
        }

    @Test
    fun testCollaboratorCount() =
        runBlocking {
            var count = 0L
            while (true) {
                val response =
                    client.get("v1/collaborations/collaborators/count") {
                        bearerAuth(testUserToken)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val actualCount = response.body<CountResponse>().count
                assertThat(actualCount).isEqualTo(count)

                if (++count == 10L) break

                // Add collaborations directly into database
                addCollaborationToDatabase(testUserId, count.toInt())
            }
        }

    @Test
    fun testCollaborationReplyAccepted() =
        runBlocking {
            // Add Collaboration invitation directly into database
            val collaborationId =
                addCollaborationToDatabase(
                    email = testUserEmail,
                    status = CollaborationStatus.Waiting
                ).id!!

            // reply
            val response =
                client.put("v1/collaborations/$collaborationId/reply") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(CollaborationReply(true))
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // check status directly from database
            val status = transaction { CollaborationEntity[collaborationId].status }
            assertThat(status).isEqualTo(CollaborationStatus.Accepted)
        }

    @Test
    fun testCollaborationReplyRejected() =
        runBlocking {
            // Add Collaboration invitation directly into database
            val collaborationId =
                addCollaborationToDatabase(
                    email = testUserEmail,
                    status = CollaborationStatus.Waiting
                ).id!!

            // reply
            val response =
                client.put("v1/collaborations/$collaborationId/reply") {
                    bearerAuth(testUserToken)
                    contentType(ContentType.Application.Json)
                    setBody(CollaborationReply(false))
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

            // check status directly from database
            val status = transaction { CollaborationEntity[collaborationId].status }
            assertThat(status).isEqualTo(CollaborationStatus.Rejected)
        }
}

private fun addCollaborationToDatabase(
    ownerId: UserId? = null,
    offset: Int = 0,
    email: String? = null,
    status: CollaborationStatus? = null
): Collaboration {
    val ownerEntityId =
        ownerId?.let {
            EntityID(it, UserTable)
        } ?: transaction {
            UserEntity.new {
                this.email = "artist$offset@newm.io"
            }
        }.id
    val songId =
        transaction {
            SongEntity
                .new {
                    this.ownerId = ownerEntityId
                    title = "Song$offset"
                    genres = arrayOf("Genre$offset")
                }.id
        }
    return transaction {
        CollaborationEntity.new {
            this.songId = songId
            this.email = email ?: "collaborator$offset@email.com"
            role = "Role$offset"
            royaltyRate = 1f / (offset + 2)
            credited = (offset % 2) == 1
            featured = (offset % 2) == 0
            this.status = status ?: CollaborationStatus.entries[offset % CollaborationStatus.entries.size]
        }
    }.toModel()
}
