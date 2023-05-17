package io.newm.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HealthTest {
    @Test
    fun `healthcheck request`() = runBlocking {
        val response = TestContext.client.get("${TestContext.baseUrl}/contents/predefined-genres.json")
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }
}
