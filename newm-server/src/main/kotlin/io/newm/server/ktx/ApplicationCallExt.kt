package io.newm.server.ktx

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationReceivePipeline
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.Memory
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.model.ClientPlatform
import io.newm.server.model.FilterCriteria
import io.newm.server.model.toFilterCriteria
import io.newm.server.model.toStringFilterCriteria
import io.newm.server.model.toUUIDFilterCriteria
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpUnauthorizedException
import io.newm.shared.ktx.orNull
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.toHexString
import io.newm.shared.ktx.toLocalDateTime
import io.newm.shared.ktx.toUUID
import java.nio.ByteBuffer
import java.security.Key
import java.time.LocalDateTime
import java.util.UUID
import javax.crypto.Mac
import kotlin.reflect.KClass
import org.jetbrains.exposed.sql.SortOrder

val ApplicationCall.jwtPrincipal: JWTPrincipal
    get() = principal()!!

val ApplicationCall.jwtId: UUID?
    get() = jwtPrincipal.jwtId?.orNull()?.toUUID()

val ApplicationCall.myUserId: UserId
    get() = jwtPrincipal.subject!!.toUUID()

val ApplicationCall.userId: UserId
    get() {
        val id = parameters["userId"]!!
        return if (id == "me") myUserId else id.toUUID()
    }

val ApplicationCall.songId: SongId
    get() = parameters["songId"]!!.toUUID()

val ApplicationCall.mintingStatus: MintingStatus
    get() = parameters["mintingStatus"]!!.let(MintingStatus::valueOf)

val ApplicationCall.playlistId: UUID
    get() = parameters["playlistId"]!!.toUUID()

val ApplicationCall.collaborationId: UUID
    get() = parameters["collaborationId"]!!.toUUID()

val ApplicationCall.collaborators: Int
    get() = parameters["collaborators"]?.toInt() ?: 1

val ApplicationCall.offset: Int
    get() = parameters["offset"]?.toInt().orZero()

val ApplicationCall.limit: Int
    get() = parameters["limit"]?.toInt() ?: 25

val ApplicationCall.sortOrder: SortOrder?
    get() = parameters["sortOrder"]?.let { SortOrder.valueOf(it.uppercase()) }

val ApplicationCall.sortedBy: String?
    get() = parameters["sortedBy"]

val ApplicationCall.ids: FilterCriteria<UUID>?
    get() = parameters["ids"]?.toUUIDFilterCriteria()

val ApplicationCall.ownerIds: FilterCriteria<UUID>?
    get() =
        parameters["ownerIds"]?.toFilterCriteria {
            if (it == "me") myUserId else it.toUUID()
        }
val ApplicationCall.songIds: FilterCriteria<UUID>?
    get() = parameters["songIds"]?.toUUIDFilterCriteria()

val ApplicationCall.emails: FilterCriteria<String>?
    get() = parameters["emails"]?.toStringFilterCriteria()

val ApplicationCall.genres: FilterCriteria<String>?
    get() = parameters["genres"]?.toStringFilterCriteria()

val ApplicationCall.moods: FilterCriteria<String>?
    get() = parameters["moods"]?.toStringFilterCriteria()

val ApplicationCall.roles: FilterCriteria<String>?
    get() = parameters["roles"]?.toStringFilterCriteria()

val ApplicationCall.olderThan: LocalDateTime?
    get() = parameters["olderThan"]?.toLocalDateTime()

val ApplicationCall.newerThan: LocalDateTime?
    get() = parameters["newerThan"]?.toLocalDateTime()

val ApplicationCall.phrase: String?
    get() = parameters["phrase"]

val ApplicationCall.archived: Boolean?
    get() = parameters["archived"]?.toBoolean()

val ApplicationCall.connectionId: UUID
    get() = parameters["connectionId"]!!.toUUID()

val ApplicationCall.saleId: UUID
    get() = parameters["saleId"]!!.toUUID()

val ApplicationCall.artistId: UserId
    get() = parameters["artistId"]!!.toUUID()

val ApplicationCall.artistIds: FilterCriteria<UserId>?
    get() = parameters["artistIds"]?.toUUIDFilterCriteria()

val ApplicationCall.clientPlatform: ClientPlatform?
    get() = parameters["clientPlatform"]?.let(ClientPlatform::valueOf)

suspend inline fun ApplicationCall.identifyUser(crossinline body: suspend ApplicationCall.(UUID, Boolean) -> Unit) {
    val uid = userId
    body(uid, uid == myUserId)
}

suspend inline fun ApplicationCall.restrictToMe(crossinline body: suspend ApplicationCall.(UUID) -> Unit) {
    identifyUser { userId, isMe ->
        if (isMe) {
            body(userId)
        } else {
            respond(HttpStatusCode.Forbidden)
        }
    }
}

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
    val mac =
        Mac.getInstance(key.algorithm).apply {
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
