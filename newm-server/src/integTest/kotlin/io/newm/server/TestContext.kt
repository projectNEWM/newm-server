package io.newm.server

import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.newm.server.auth.password.LoginRequest
import io.newm.server.auth.password.LoginResponse
import io.newm.shared.auth.Password
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object TestContext {
    val config = ConfigFactory.load("${System.getProperty("newm.env")}.conf")
    val baseUrl: String = config.getString("newm.baseUrl")
    val email: String = config.getString("newm.email")
    val password: String = config.getString("newm.password")
    var loginResponse: LoginResponse
    val client: HttpClient = HttpClient() {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    init {
        runBlocking {
            // Go ahead and login when test starts so we have access to accessToken and refreshToken
            // this kind of defeats the point of having an Auth success test, but couldn't think of another way
            // right now
            val response = client.post("$baseUrl/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    LoginRequest(
                        email = email,
                        password = Password(password)
                    )
                )
            }
            loginResponse = response.body()
        }
    }
}
