package io.newm.server.features.idenfy.repo

import io.newm.server.features.idenfy.model.IdenfyRequest

interface IdenfyRepository {
    suspend fun processRequest(request: IdenfyRequest)
}
