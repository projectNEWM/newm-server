package io.newm.server.ktx

import com.auth0.jwt.interfaces.Verification

fun Verification.withAnyOfIssuer(issuers: List<String>): Verification =
    withClaim("iss") { claim, _ ->
        claim.asString() in issuers
    }

fun Verification.withAnyOfAudience(audiences: List<String>): Verification = withAnyOfAudience(*audiences.toTypedArray())
