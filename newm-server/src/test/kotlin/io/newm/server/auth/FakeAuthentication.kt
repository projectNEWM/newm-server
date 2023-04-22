package io.newm.server.auth

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.parseAuthorizationHeader
import io.newm.server.features.idenfy.configIdenfyFakeServerAuth
import java.util.Date

// Fake JWK authentication using User ID passed as access token
fun Application.installFakeAuthentication() {
    install(Authentication) {
        register(FakeAuthProvider("auth-jwt"))
        register(FakeAuthProvider("auth-jwt-refresh"))
        configIdenfyFakeServerAuth()
    }
}

private class FakeAuthProvider(name: String) : AuthenticationProvider(object : Config(name) {}) {
    override suspend fun onAuthenticate(context: AuthenticationContext) = context.run {
        val authHeader = call.request.parseAuthorizationHeader() as HttpAuthHeader.Single
        principal(JWTPrincipal(FakePayload(authHeader.blob)))
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

    override fun getClaim(name: String?): Claim = object : Claim {
        override fun isNull(): Boolean = true
        override fun isMissing(): Boolean = true
        override fun asBoolean(): Boolean? = null
        override fun asInt(): Int? = null
        override fun asLong(): Long? = null
        override fun asDouble(): Double? = null
        override fun asString(): String? = null
        override fun asDate(): Date? = null
        override fun <T : Any?> asArray(clazz: Class<T>?): Array<T>? = null
        override fun <T : Any?> asList(clazz: Class<T>?): MutableList<T>? = null
        override fun asMap(): MutableMap<String, Any>? = null
        override fun <T : Any?> `as`(clazz: Class<T>?): T? = null
    }

    override fun getClaims(): MutableMap<String, Claim> = mutableMapOf()
}
