package io.newm.shared.ktx

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put

inline fun Route.get(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = get(path) { body(call) }

inline fun Route.get(crossinline body: suspend ApplicationCall.() -> Unit) = get { body(call) }

inline fun Route.put(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = put(path) { body(call) }

inline fun Route.put(crossinline body: suspend ApplicationCall.() -> Unit) = put { body(call) }

inline fun Route.post(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = post(path) { body(call) }

inline fun Route.post(crossinline body: suspend ApplicationCall.() -> Unit) = post { body(call) }

inline fun Route.patch(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = patch(path) { body(call) }

inline fun Route.patch(crossinline body: suspend ApplicationCall.() -> Unit) = patch { body(call) }

inline fun Route.delete(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = delete(path) { body(call) }

inline fun Route.delete(crossinline body: suspend ApplicationCall.() -> Unit) = delete { body(call) }

inline fun Route.head(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = head(path) { body(call) }

inline fun Route.head(crossinline body: suspend ApplicationCall.() -> Unit) = head { body(call) }

inline fun Route.options(
    path: String,
    crossinline body: suspend ApplicationCall.() -> Unit
) = options(path) { body(call) }

inline fun Route.options(crossinline body: suspend ApplicationCall.() -> Unit) = options { body(call) }
