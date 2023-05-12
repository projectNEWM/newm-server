package io.newm.server.ktx

import com.auth0.jwt.interfaces.Verification

fun Verification.withAnyOfAudience(audiences: List<String>) = withAnyOfAudience(*audiences.toTypedArray())
