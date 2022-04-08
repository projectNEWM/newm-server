package io.projectnewm.server.portal

import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.projectnewm.server.content.installContentNegotiation
import io.projectnewm.server.di.installDependencyInjection
import io.projectnewm.server.features.song.createSongRoutes
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.song.repo.mockSongs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PortalModuleTest {
    @Test
    fun testGetSongs() {
        withTestApplication({
            installContentNegotiation()
            installDependencyInjection()
            installFakeAuthentication()
            routing {
                createSongRoutes()
            }
        }) {
            handleRequest(HttpMethod.Get, "v1/songs").apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
                assertThat(response.content).isNotEmpty()
                val songs = Json.decodeFromString<List<Song>>(response.content!!)
                assertThat(songs).isEqualTo(mockSongs)
            }
        }
    }
}
