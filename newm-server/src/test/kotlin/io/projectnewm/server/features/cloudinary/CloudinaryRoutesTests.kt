package io.projectnewm.server.features.cloudinary

import com.cloudinary.Cloudinary
import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.projectnewm.server.BaseApplicationTests
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.value
import io.projectnewm.server.features.cloudinary.model.CloudinarySignResponse
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

class CloudinaryRoutesTests : BaseApplicationTests() {

    @Test
    fun testSign() = runBlocking {

        val start = Instant.now().epochSecond

        val params = mapOf(
            "string1" to JsonPrimitive("test"),
            "double1" to JsonPrimitive(Math.PI),
            "long1" to JsonPrimitive(Long.MAX_VALUE),
            "bool1" to JsonPrimitive(true),
            "bool2" to JsonPrimitive(false)
        )

        val response = client.post("v1/cloudinary/sign") {
            bearerAuth(testUserToken)
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val resp = response.body<CloudinarySignResponse>()
        val cloudinary by inject<Cloudinary>()
        assertThat(resp.cloudName).isEqualTo(cloudinary.config.cloudName)
        assertThat(resp.apiKey).isEqualTo(cloudinary.config.apiKey)
        assertThat(resp.timestamp).isAtLeast(start)

        val expSignature = cloudinary.apiSignRequest(
            mutableMapOf<String, Any>().apply {
                params.mapValuesTo(this) { it.value.value }
                put("timestamp", resp.timestamp)
            },
            cloudinary.config.apiSecret
        )
        assertThat(resp.signature).isEqualTo(expSignature)
    }
}
