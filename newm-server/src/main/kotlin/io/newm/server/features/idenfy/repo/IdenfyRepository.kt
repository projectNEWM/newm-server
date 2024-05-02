package io.newm.server.features.idenfy.repo

import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.server.features.idenfy.model.IdenfySessionResult
import io.newm.server.typealiases.UserId

interface IdenfyRepository {
    suspend fun createSession(userId: UserId): IdenfyCreateSessionResponse

    suspend fun processSessionResult(result: IdenfySessionResult)
}
