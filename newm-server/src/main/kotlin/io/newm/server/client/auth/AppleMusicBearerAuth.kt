package io.newm.server.client.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.security.KeyParser
import io.newm.shared.koin.inject
import java.security.KeyFactory
import java.security.interfaces.ECKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

fun AuthConfig.appleMusicBearer() =
    bearer {
        val loader = AppleMusicTokenLoader()

        // Load and refresh tokens without waiting for a 401 first if the host matches
        sendWithoutRequest { request ->
            request.url.host == "api.music.apple.com"
        }
        loadTokens {
            loader.load()
        }
        refreshTokens {
            loader.tokens = null
            loader.load()
        }
    }

private class AppleMusicTokenLoader {
    private val environment: ApplicationEnvironment by inject()
    var tokens: BearerTokens? = null

    suspend fun load(): BearerTokens {
        tokens?.accessToken?.let { jwtToken ->
            val decodedToken = JWT.decode(jwtToken)
            if (decodedToken.expiresAt.toInstant().isAfter(Instant.now().plus(5L, ChronoUnit.MINUTES))) {
                // Token is still valid for over 5 minutes
                return tokens!!
            }
        }

        val keyBytes = KeyParser.parse(environment.getSecureConfigString("jwt.apple.musickit.privateKey"))
        val teamId = environment.getSecureConfigString("jwt.apple.musickit.teamId")
        val keyId = environment.getSecureConfigString("jwt.apple.musickit.keyId")
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val privateKey: ECKey = KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECKey
        val algorithm = Algorithm.ECDSA256(privateKey)
        val token =
            JWT
                .create()
                .withKeyId(keyId)
                .withIssuer(teamId)
                .withExpiresAt(Date.from(Instant.now().plus(1L, ChronoUnit.HOURS)))
                .withIssuedAt(Date.from(Instant.now()))
                .sign(algorithm)

        return BearerTokens(token, token).also { tokens = it }
    }
}
