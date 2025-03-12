package io.newm.server.features.dripdropz.repo

import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.dripdropz.model.AvailableToken
import io.newm.server.features.dripdropz.model.CheckRequest
import io.newm.server.features.dripdropz.model.CheckResponse
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.getConfigString

class DripDropzRepositoryImpl(
    private val client: HttpClient,
    private val environment: ApplicationEnvironment
) : DripDropzRepository {
    override suspend fun checkAvailableTokens(address: String): List<AvailableToken> {
        val apiUrl = environment.getConfigString("dripDropz.apiUrl")
        val appId = environment.getSecureConfigString("dripDropz.appId")
        val accessToken = environment.getSecureConfigString("dripDropz.accessToken")
        return client
            .post("$apiUrl/drip/check") {
                retry {
                    maxRetries = 2
                    delayMillis { 500L }
                }
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("X-App-Id", appId)
                header("X-Access-Token", accessToken)
                setBody(CheckRequest(address))
            }.checkedBody<CheckResponse>()
            .data.availableTokens
    }
}
