package io.projectnewm.server.pugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.projectnewm.server.pugins.auth.OAuthType
import io.projectnewm.server.pugins.auth.configureJwt
import io.projectnewm.server.pugins.auth.configureOAuth
import io.projectnewm.server.pugins.auth.routeOAuth
import io.projectnewm.server.user.UserRepository
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h6
import kotlinx.html.p

fun Application.configureAuthentication(repository: UserRepository) {
    install(Authentication) {
        configureOAuth(OAuthType.Google, environment)
        configureOAuth(OAuthType.Facebook, environment)
        configureOAuth(OAuthType.LinkedIn, environment)
        configureJwt(environment)
    }
    routing {
        routeOAuth(OAuthType.Google, repository)
        routeOAuth(OAuthType.Facebook, repository)
        routeOAuth(OAuthType.LinkedIn, repository)

        // TODO: remove these temporary test pages
        get("/") {
            call.respondHtml {
                body {
                    h1 {
                        text("Welcome to NEWM Server")
                    }
                    h6 {
                        text("Herouku App Name: ${System.getenv("HEROKU_APP_NAME")}")
                    }
                    p {
                        a("/login") { +"Login" }
                    }
                    p {
                        a("/logout") { +"Logout" }
                    }
                    p {
                        a("/portal/songs") { +"Test Get Portal Songs (expect 401 if logged out)" }
                    }
                    p {
                        a("/mobile") { +"Test Get Mobile Placeholder (expect 401 if logged out)" }
                    }
                    p {
                        a("/jwt") { +"Test JWT (expect 401 if logged out)" }
                    }
                }
            }
        }
        get("/login") {
            call.respondHtml {
                body {
                    p {
                        a("/login-google") { +"Login with Google" }
                    }
                    p {
                        a("/login-facebook") { +"Login with Facebook" }
                    }
                    p {
                        a("/login-linkedin") { +"Login with LinkedIn" }
                    }
                }
            }
        }
        get("/logout") {
            call.sessions.clear()
            call.respondText("Successfully logged out!!!")
        }
        authenticate("auth-jwt") {
            get("/jwt") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respondHtml {
                    body {
                        h1 {
                            text("JWT")
                        }
                        p {
                            text("Issuer: ${principal.issuer}")
                        }
                        p {
                            text("Audience: ${principal.audience}")
                        }
                        p {
                            text("Subject: ${principal.subject}")
                        }
                        p {
                            text("ExpiresAt: ${principal.expiresAt}")
                        }
                    }
                }
            }
        }
    }
}
