package io.newm.chain.grpc

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.ktor.server.config.ApplicationConfig
import io.ktor.utils.io.core.toByteArray
import io.newm.chain.database.entity.User
import io.newm.chain.database.repository.UsersRepository
import io.newm.server.di.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class JwtAuthorizationServerInterceptor(jwtConfig: ApplicationConfig) : ServerInterceptor {

    private val log: Logger by inject { parametersOf("JwtAuthorizationServerInterceptor") }
    private val usersRepository: UsersRepository by inject()

    private val issuer by lazy {
        jwtConfig.property("domain").getString()
    }
    private val audience by lazy {
        jwtConfig.property("audience").getString()
    }
    private val algorithm by lazy {
        val secret = MessageDigest.getInstance("SHA-512").digest(jwtConfig.property("secret").getString().toByteArray())
        Algorithm.HMAC256(secret)
    }
    private val durationYears by lazy {
        jwtConfig.property("durationYears").getString().toLong()
    }

    private val jwtVerifier: JWTVerifier by lazy {
        JWT.require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val status: Status = headers.get(AUTHORIZATION_METADATA_KEY)?.let { authToken ->
            if (!authToken.startsWith(BEARER_TYPE)) {
                Status.UNAUTHENTICATED.withDescription("Unknown authorization type")
            } else {
                val jwtBearerToken = authToken.substring(BEARER_TYPE.length).trim()
                try {
                    val jwt = jwtVerifier.verify(jwtBearerToken)
                    jwt.claims["user"]?.asString()?.let { user ->
                        val jwtUser = "$user|jwt"
                        usersRepository.getByName(jwtUser)?.let {
                            val ctx = Context.current().withValue(CLIENT_ID_CONTEXT_KEY, user)
                            return Contexts.interceptCall(ctx, call, headers, next)
                        } ?: Status.UNAUTHENTICATED.withDescription("Missing JWT user account")
                    } ?: Status.UNAUTHENTICATED.withDescription("JWT has no user claim")
                } catch (e: JWTVerificationException) {
                    Status.UNAUTHENTICATED.withCause(e)
                }
            }
        } ?: Status.UNAUTHENTICATED.withDescription("Authorization token is missing")

        call.close(status, headers)
        return object : ServerCall.Listener<ReqT>() {
            // noop
        }
    }

    fun createJwtUser(name: String) {
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("user", name)
            .withExpiresAt(Date.from(Instant.now().plus(durationYears * 365, ChronoUnit.DAYS)))
            .withIssuedAt(Date.from(Instant.now()))
            .sign(algorithm)

        log.warn("JWT token: $token")
        val jwtUser = "$name|jwt"
        usersRepository.getByName(jwtUser) ?: run {
            usersRepository.insert(User(name = jwtUser))
        }
    }

    companion object {
        const val BEARER_TYPE = "Bearer"
        val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER)
        val CLIENT_ID_CONTEXT_KEY: Context.Key<String> = Context.key("clientId")
    }
}
