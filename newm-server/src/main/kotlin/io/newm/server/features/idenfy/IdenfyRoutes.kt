package io.newm.server.features.idenfy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.newm.server.ext.receiveAndVerify
import io.newm.server.features.idenfy.repo.IdenfyRepository
import org.koin.ktor.ext.inject
import java.security.Key

fun Routing.createIdenfyRoutes() {
    val key: Key by inject(IDENFY_KEY_QUALIFIER)
    val repository: IdenfyRepository by inject()

    post("/v1/idenfy/callback") {
        with(call) {
            repository.processRequest(
                receiveAndVerify(
                    signatureHeader = "Idenfy-Signature",
                    key = key
                )
            )
            respond(HttpStatusCode.OK)
        }
    }
}
