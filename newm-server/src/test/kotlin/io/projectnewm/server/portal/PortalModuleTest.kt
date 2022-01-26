package io.projectnewm.server.portal

import com.google.common.truth.Truth.assertThat
import io.ktor.http.*
import io.ktor.server.testing.*
import io.projectnewm.server.mainModule
import io.projectnewm.server.portal.model.GetSongsResponse
import io.projectnewm.server.portal.repo.mockSongs
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test

class PortalModuleTest {
    @Test
    fun testGetSongs() {
        withTestApplication({
            mainModule()
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
