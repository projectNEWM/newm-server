package io.newm.server.features.dripdropz.repo

import io.newm.server.features.dripdropz.model.AvailableToken

interface DripDropzRepository {
    suspend fun checkAvailableTokens(address: String): List<AvailableToken>
}
