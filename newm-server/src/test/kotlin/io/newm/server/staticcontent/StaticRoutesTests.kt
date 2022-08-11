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
    fun testGetPredefinedRoles() = runBlocking {
        val response = client.get("contents/predefined-roles.json")
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()).isEqualTo(ContentType.Application.Json)

        val content = javaClass.getResource("/static/predefined-roles.json").readText()
        assertThat(response.body<String>()).isEqualTo(content)
    }

    @Test
    fun testGetPredefinedGenres() = runBlocking {
        val response = client.get("contents/predefined-genres.json")
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()).isEqualTo(ContentType.Application.Json)

        val content = javaClass.getResource("/static/predefined-genres.json").readText()
        assertThat(response.body<String>()).isEqualTo(content)
    }
}
