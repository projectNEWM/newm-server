package io.projectnewm.server.plugins.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.call
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.oauth
import io.ktor.server.auth.principal
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.ktor.server.util.url
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.getConfigStrings
import io.projectnewm.server.koin.inject
import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.sessions.token
import io.projectnewm.server.user.UserRepository

fun Authentication.Configuration.configureOAuth(
    type: OAuthType,
    environment: ApplicationEnvironment
) {
    val name = type.name.lowercase()
    val authUrl = System.getenv("HEROKU_APP_NAME")?.let { "https://$it.herokuapp.com/auth-$name" }
    oauth("auth-oauth-$name") {
        urlProvider = {
            authUrl ?: url {
                pathSegments = listOf("auth-$name")
                parameters.clear()
            }
        }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = name,
                authorizeUrl = environment.getConfigString("oauth.$name.authorizeUrl"),
                accessTokenUrl = environment.getConfigString("oauth.$name.accessTokenUrl"),
                requestMethod = HttpMethod.Post,
                clientId = environment.getConfigString("oauth.$name.clientId"),
                clientSecret = environment.getConfigString("oauth.$name.clientSecret"),
                defaultScopes = environment.getConfigStrings("oauth.$name.defaultScopes")
            )
        }
        client = HttpClient(CIO)
    }
}

fun Routing.routeOAuth(type: OAuthType) {
    val repository: UserRepository by inject()
    val name = type.name.lowercase()
    authenticate("auth-oauth-$name") {
        get("/login-$name") {
            // Redirects to 'authorizeUrl' automatically
        }

        get("/auth-$name") {
            with(call) {
                val accessToken = principal<OAuthAccessTokenResponse.OAuth2>()?.accessToken
                if (accessToken != null) {
                    call.sessions.token = createJwtToken(
                        subject = repository.registerUser(type, accessToken),
                        environment = application.environment
                    )
                    respondRedirect("/")
                } else {
                    response.status(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
