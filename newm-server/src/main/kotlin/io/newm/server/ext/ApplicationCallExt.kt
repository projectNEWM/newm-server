package io.newm.server.ext

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

val ApplicationCall.ownerId: UUID?
    get() {
        val id = parameters["ownerId"]
        return if (id == "me") myUserId else id?.toUUID()
    }

val ApplicationCall.genre: String?
    get() = parameters["genre"]

val ApplicationCall.offset: Int
    get() = parameters["offset"]?.toInt() ?: 0

val ApplicationCall.limit: Int
    get() = parameters["limit"]?.toInt() ?: 25
