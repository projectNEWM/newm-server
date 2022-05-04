package io.projectnewm.server.auth

import com.auth0.jwt.impl.NullClaim
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationPipeline
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.parseAuthorizationHeader
import java.util.Date

// Fake JWK authentication using User ID passed as access token
fun Application.installFakeAuthentication() {
    install(Authentication) {
        fakeJwtAuthProvider("auth-jwt")
        fakeJwtAuthProvider("auth-jwt-refresh")
    }
}

private fun Authentication.Configuration.fakeJwtAuthProvider(name: String) {
    AuthenticationProvider(object : AuthenticationProvider.Configuration(name) {}).apply {
        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            val authHeader = context.call.request.parseAuthorizationHeader() as HttpAuthHeader.Single
            context.principal(JWTPrincipal(FakePayload(authHeader.blob)))
        }
        register(this)
    }
}

private class FakePayload(private val userId: String) : Payload {
    override fun getIssuer(): String = ""

    override fun getSubject() = userId

    override fun getAudience(): MutableList<String> = mutableListOf()

    override fun getExpiresAt(): Date = Date()

    override fun getNotBefore(): Date = Date()

    override fun getIssuedAt(): Date = Date()

    override fun getId(): String = ""

    override fun getClaim(name: String?): Claim = NullClaim()

    override fun getClaims(): MutableMap<String, Claim> = mutableMapOf()
}
