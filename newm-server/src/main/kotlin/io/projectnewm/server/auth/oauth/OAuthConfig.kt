package io.projectnewm.server.auth.oauth

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.util.url
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.ext.getConfigStrings
import io.projectnewm.server.koin.inject

fun Authentication.Configuration.configureOAuth(type: OAuthType) {
    val environment: ApplicationEnvironment by inject()
    val httpClient: HttpClient by inject()
    val name = type.name.lowercase()
    val authUrl = System.getenv("HEROKU_APP_NAME")?.let { "https://$it.herokuapp.com/auth/$name" }

    oauth("auth-oauth-$name") {
        urlProvider = {
            authUrl ?: url {
                pathSegments = listOf("auth", name)
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
        client = httpClient
    }
}
