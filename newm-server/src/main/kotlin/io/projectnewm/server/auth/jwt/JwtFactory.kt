package io.projectnewm.server.auth.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.projectnewm.server.ext.getConfigLong
import io.projectnewm.server.ext.getConfigString
import java.util.Date

fun Application.createJwt(subject: String): String = environment.run {
    JWT.create()
        .withIssuer(getConfigString("jwt.issuer"))
        .withAudience(getConfigString("jwt.audience"))
        .withSubject(subject)
        .withExpiresAt(Date(System.currentTimeMillis() + getConfigLong("jwt.timeToLive")))
        .sign(Algorithm.HMAC256(getConfigString("jwt.secret")))
}
