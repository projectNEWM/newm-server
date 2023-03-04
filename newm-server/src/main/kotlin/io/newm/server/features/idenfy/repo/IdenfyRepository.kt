package io.newm.server.features.idenfy.repo

import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.server.features.idenfy.model.IdenfySessionResult
import java.util.UUID

interface IdenfyRepository {
    suspend fun createSession(userId: UUID): IdenfyCreateSessionResponse

    suspend fun processSessionResult(result: IdenfySessionResult)
}
