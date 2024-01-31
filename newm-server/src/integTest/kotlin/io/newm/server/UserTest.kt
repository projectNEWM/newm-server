package io.newm.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.newm.server.features.user.model.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class UserTest {
    @Test
    fun `me endpoint returns success`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/users/me") {
                    headers {
                        append("Authorization", "Bearer ${TestContext.loginResponse.accessToken}")
                        accept(ContentType.Application.Json)
                    }
                }
            val user: User = response.body()
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(user.email).isEqualTo(TestContext.email)
        }

    @Test
    fun `me endpoint invalid auth returns unauthorized`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/users/me") {
                    headers {
                        append("Authorization", "Bearer INVALIDACCESSTOKEN")
                        accept(ContentType.Application.Json)
                    }
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        }

    @Test
    fun `Unrecognized user id returns Not Found`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/users/00000000-0000-0000-0000-000000000000") {
                    headers {
                        append("Authorization", "Bearer ${TestContext.loginResponse.accessToken}")
                        accept(ContentType.Application.Json)
                    }
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
        }

    @Test
    fun `invalid user id returns Unprocessable Entity`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/users/-1") {
                    headers {
                        append("Authorization", "Bearer ${TestContext.loginResponse.accessToken}")
                        accept(ContentType.Application.Json)
                    }
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
        }
}
