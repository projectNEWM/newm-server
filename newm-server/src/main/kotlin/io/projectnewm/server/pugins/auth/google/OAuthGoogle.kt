package io.projectnewm.server.pugins.auth.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Cookie
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.call
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.oauth
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.getConfigStrings

fun Authentication.Configuration.configureOAuthGoogle(environment: ApplicationEnvironment) {
    val herokuUrl = System.getenv("HEROKU_APP_NAME")?.let { "https://$it.herokuapp.com/auth-google" }
    oauth("auth-oauth-google") {
        urlProvider = {
            herokuUrl ?: request.origin.run {
                URLBuilder(
                    protocol = URLProtocol.createOrDefault(scheme),
                    host = host,
                    port = port,
                    pathSegments = listOf("auth-google")
                ).buildString()
            }
        }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "google",
                authorizeUrl = environment.getConfigString("oauth.google.authorizeUrl"),
                accessTokenUrl = environment.getConfigString("oauth.google.accessTokenUrl"),
                requestMethod = HttpMethod.Post,
                clientId = environment.getConfigString("oauth.google.clientId"),
                clientSecret = environment.getConfigString("oauth.google.clientSecret"),
                defaultScopes = environment.getConfigStrings("oauth.google.defaultScopes")
            )
        }
        client = HttpClient(CIO)
    }
}

fun Routing.routeOAuthGoogle() {
    authenticate("auth-oauth-google") {
        get("/login-google") {
            // Redirects to 'authorizeUrl' automatically
        }

        get("/auth-google") {
            with(call) {
                principal<OAuthAccessTokenResponse.OAuth2>()?.extraParameters?.get("id_token")?.let {
                    response.cookies.append(Cookie("token", it))
                }
                respondRedirect("/")
            }
        }
    }
}
