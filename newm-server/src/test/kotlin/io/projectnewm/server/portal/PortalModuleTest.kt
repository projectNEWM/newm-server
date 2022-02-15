package io.projectnewm.server.portal

import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.projectnewm.server.portal.model.GetSongsResponse
import io.projectnewm.server.portal.repo.mockSongs
import io.projectnewm.server.pugins.configureSerialization
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PortalModuleTest {
    @Test
    fun testGetSongs() {
        withTestApplication({
            configureSerialization()
            configureFakeAuthentication()
            portalModule()
        }) {
            handleRequest(HttpMethod.Get, "/portal/songs").apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                assertThat(response.content).isNotEmpty()
                val response = Json.decodeFromString<GetSongsResponse>(response.content!!)
                assertThat(response.version).isEqualTo(1)
                assertThat(response.songs).isEqualTo(mockSongs)
            }
        }
    }
}
