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
import io.projectnewm.server.features.cloudinary.model.CloudinarySignResponse
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CloudinaryRoutesTests : BaseApplicationTests() {

    @Test
    fun testSign() = runBlocking {

        val start = Instant.now().epochSecond

        val params = mapOf(
            "folder" to "test",
            "format" to "jpg",
            "eager" to "c_pad,h_300,w_400|c_crop,h_200,w_260"
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
                putAll(params)
                put("timestamp", resp.timestamp)
            },
            cloudinary.config.apiSecret
        )
        assertThat(resp.signature).isEqualTo(expSignature)
    }
}
