package io.newm.server.ext

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*
import io.newm.shared.exception.HttpUnauthorizedException
import io.newm.shared.ext.splitAndTrim
import io.newm.shared.ext.toHexString
import io.newm.shared.ext.toLocalDateTime
import io.newm.shared.ext.toUUID
import java.nio.ByteBuffer
import java.security.Key
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import kotlin.reflect.KClass

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

val ApplicationCall.offset: Int
    get() = parameters["offset"]?.toInt() ?: 0

val ApplicationCall.limit: Int
    get() = parameters["limit"]?.toInt() ?: 25

val ApplicationCall.ids: List<UUID>?
    get() = parameters["ids"]?.splitAndTrim()?.map { it.toUUID() }

val ApplicationCall.ownerIds: List<UUID>?
    get() = parameters["ownerIds"]?.splitAndTrim()?.map {
        if (it == "me") myUserId else it.toUUID()
    }

val ApplicationCall.genres: List<String>?
    get() = parameters["genres"]?.splitAndTrim()

val ApplicationCall.roles: List<String>?
    get() = parameters["roles"]?.splitAndTrim()

val ApplicationCall.olderThan: LocalDateTime?
    get() = parameters["olderThan"]?.toLocalDateTime()

val ApplicationCall.newerThan: LocalDateTime?
    get() = parameters["newerThan"]?.toLocalDateTime()

/***
 * Receives signed content and verifies signature
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveAndVerify(
    signatureHeader: String,
    key: Key
): T = receiveAndVerify(signatureHeader, key, T::class)

/***
 * Receives signed content and verifies signature
 */
suspend fun <T : Any> ApplicationCall.receiveAndVerify(
    signatureHeader: String,
    key: Key,
    type: KClass<T>
): T {
    val signature = request.headers[signatureHeader] ?: throw HttpUnauthorizedException("missing signature")
    val mac = Mac.getInstance(key.algorithm).apply {
        init(key)
    }
    request.pipeline.intercept(ApplicationReceivePipeline.Before) { data ->
        val channel = data as ByteReadChannel
        val size = channel.availableForRead
        val buffer = ByteBuffer.allocate(size)
        channel.peekTo(Memory(buffer), 0, 0, size.toLong(), size.toLong())
        mac.update(buffer)
        if (mac.doFinal().toHexString() != signature) {
            throw HttpUnauthorizedException("invalid signature")
        }
        proceed()
    }
    return receive(type)
}
