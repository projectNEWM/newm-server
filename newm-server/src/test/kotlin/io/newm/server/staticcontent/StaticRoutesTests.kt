package io.newm.server.staticcontent

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class StaticRoutesTests : BaseApplicationTests() {
    @Test
    fun testGetPredefinedMoods() =
        runBlocking {
            val response = client.get("contents/predefined-moods.json")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.contentType()).isEqualTo(ContentType.Application.Json)

            val content = javaClass.getResource("/static/predefined-moods.json").readText()
            assertThat(response.body<String>()).isEqualTo(content)
        }

    @Test
    fun testGetStreamTokenPolicyIds() =
        runBlocking {
            val response = client.get("contents/stream-token-policy-ids.json")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.contentType()).isEqualTo(ContentType.Application.Json)

            val content = javaClass.getResource("/static/stream-token-policy-ids.json").readText()
            assertThat(response.body<String>()).isEqualTo(content)
        }

    @Test
    fun testGetIsrcCountryCodes() =
        runBlocking {
            val response = client.get("contents/isrc-country-codes.json")
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.contentType()).isEqualTo(ContentType.Application.Json)

            val content = javaClass.getResource("/static/isrc-country-codes.json").readText()
            assertThat(response.body<String>()).isEqualTo(content)
        }
}
