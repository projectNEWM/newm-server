package io.newm.server.features.idenfy

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.*
import io.ktor.http.*
import io.newm.server.BaseApplicationTests
import io.newm.server.di.inject
import io.newm.server.ext.toHexString
import io.newm.server.features.idenfy.model.IdenfyRequest
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.model.UserVerificationStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.security.Key
import javax.crypto.Mac

class IdenfyRoutesTests : BaseApplicationTests() {

    @Test
    fun testPostVerified() = runBlocking {
        val json: Json by inject()
        val key: Key by inject(IDENFY_KEY_QUALIFIER)

        transaction {
            UserEntity[testUserId].verificationStatus = UserVerificationStatus.Unverified
        }

        val request = json.encodeToString(
            IdenfyRequest(
                clientId = testUserId.toString(),
                isFinal = false,
                status = IdenfyRequest.Status(
                    overall = "APPROVED",
                    autoDocument = null,
                    autoFace = null,
                    fraudTags = null,
                    manualDocument = null,
                    manualFace = null,
                    mismatchTags = null,
                    suspicionReasons = null
                )
            )
        )

        val response = client.post("v1/idenfy/callback") {
            contentType(ContentType.Application.Json)
            header("Idenfy-Signature", request.sign(key))
            setBody(request)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val newStatus = transaction {
            UserEntity[testUserId].verificationStatus
        }
        assertThat(newStatus).isEqualTo(UserVerificationStatus.Verified)
    }

    @Test
    fun testPostPending() = runBlocking {
        val json: Json by inject()
        val key: Key by inject(IDENFY_KEY_QUALIFIER)

        transaction {
            UserEntity[testUserId].verificationStatus = UserVerificationStatus.Unverified
        }

        val request = json.encodeToString(
            IdenfyRequest(
                clientId = testUserId.toString(),
                isFinal = false,
                status = IdenfyRequest.Status(
                    overall = "REVIEWING",
                    autoDocument = null,
                    autoFace = null,
                    fraudTags = null,
                    manualDocument = null,
                    manualFace = null,
                    mismatchTags = null,
                    suspicionReasons = null
                )
            )
        )

        val response = client.post("v1/idenfy/callback") {
            contentType(ContentType.Application.Json)
            header("Idenfy-Signature", request.sign(key))
            setBody(request)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val newStatus = transaction {
            UserEntity[testUserId].verificationStatus
        }
        assertThat(newStatus).isEqualTo(UserVerificationStatus.Pending)
    }

    @Test
    fun testPostUnverified() = runBlocking {
        val json: Json by inject()
        val key: Key by inject(IDENFY_KEY_QUALIFIER)

        transaction {
            UserEntity[testUserId].verificationStatus = UserVerificationStatus.Pending
        }

        val request = json.encodeToString(
            IdenfyRequest(
                clientId = testUserId.toString(),
                isFinal = true,
                status = IdenfyRequest.Status(
                    overall = "DENIED",
                    autoDocument = null,
                    autoFace = null,
                    fraudTags = null,
                    manualDocument = null,
                    manualFace = null,
                    mismatchTags = null,
                    suspicionReasons = null
                )
            )
        )

        val response = client.post("v1/idenfy/callback") {
            contentType(ContentType.Application.Json)
            header("Idenfy-Signature", request.sign(key))
            setBody(request)
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val newStatus = transaction {
            UserEntity[testUserId].verificationStatus
        }
        assertThat(newStatus).isEqualTo(UserVerificationStatus.Unverified)
    }
}

private fun String.sign(key: Key): String = with(Mac.getInstance(key.algorithm)) {
    init(key)
    doFinal(toByteArray()).toHexString()
}
