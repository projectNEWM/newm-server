package io.projectnewm.server.ext

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import java.util.UUID

val ApplicationCall.myUserId: UUID
    get() = principal<JWTPrincipal>()?.subject!!.toUUID()

val ApplicationCall.userId: UUID
    get() {
        val id = parameters["userId"]!!
        return if (id == "me") myUserId else id.toUUID()
    }

suspend fun ApplicationCall.respondRedirectOrNoContent() =
    request.queryParameters["redirectUrl"]?.let { respondRedirect(it) } ?: respond(HttpStatusCode.NoContent)
