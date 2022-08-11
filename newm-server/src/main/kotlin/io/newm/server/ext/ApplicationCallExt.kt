package io.projectnewm.server.ext

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

val ApplicationCall.jwtPrincipal: JWTPrincipal
    get() = principal()!!

val ApplicationCall.jwtId: UUID
    get() = jwtPrincipal.jwtId!!.toUUID()

val ApplicationCall.myUserId: UUID
    get() = jwtPrincipal.subject!!.toUUID()

val ApplicationCall.userId: UUID
    get() {
        val id = parameters["userId"]!!
        return if (id == "me") myUserId else id.toUUID()
    }

val ApplicationCall.songId: UUID
    get() = parameters["songId"]!!.toUUID()

val ApplicationCall.playlistId: UUID
    get() = parameters["playlistId"]!!.toUUID()
