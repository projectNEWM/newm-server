package io.newm.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.auth.jwt.JwtData
import io.newm.server.auth.password.LoginRequest
import io.newm.shared.auth.Password
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class AuthTest {
    @Test
    fun `Missing auth returns unauthorized`() =
        runBlocking {
            val response = TestContext.client.get("${TestContext.baseUrl}/v1/songs")
            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        }

    @Test
    fun `Login with email and password returns Success`() =
        runBlocking {
            val response =
                TestContext.client.post("${TestContext.baseUrl}/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginRequest(
                            email = TestContext.email,
                            password = Password(TestContext.password)
                        )
                    )
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        }

    @Test
    fun `Login with unregistered email returns Not Found`() =
        runBlocking {
            val response =
                TestContext.client.post("${TestContext.baseUrl}/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginRequest(
                            email = String.randomString(),
                            password = Password("password")
                        )
                    )
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
        }

    @Test
    fun `Login with email and wrong password returns Unauthorized`() =
        runBlocking {
            val response =
                TestContext.client.post("${TestContext.baseUrl}/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginRequest(
                            email = TestContext.email,
                            password = Password("password")
                        )
                    )
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        }

    @Test
    fun `Decode JWT Access Token`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/auth/jwt") {
                    headers {
                        append("Authorization", "Bearer ${TestContext.loginResponse.accessToken}")
                        accept(ContentType.Application.Json)
                    }
                }
            val jwt: JwtData = response.body()
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(jwt.issuer).isEqualTo("https://newm.io")
        }

    @Test
    fun `Invalid JWT Access Token returns unauthorized`() =
        runBlocking {
            val response =
                TestContext.client.get("${TestContext.baseUrl}/v1/auth/jwt") {
                    headers {
                        append("Authorization", "Bearer INVALIDACCESSTOKEN")
                        accept(ContentType.Application.Json)
                    }
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        }
}
