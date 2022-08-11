package io.projectnewm.server.ext

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.util.KtorDsl
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID

@KtorDsl
suspend inline fun PipelineContext<Unit, ApplicationCall>.identifyUser(
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(UUID, Boolean) -> Unit
) {
    val userId = call.userId
    body(userId, userId == call.myUserId)
}

@KtorDsl
suspend inline fun PipelineContext<Unit, ApplicationCall>.restrictToMe(
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(UUID) -> Unit
) {
    identifyUser { userId, isMe ->
        if (isMe) {
            body(userId)
        } else {
            call.respond(HttpStatusCode.Forbidden)
        }
    }
}
