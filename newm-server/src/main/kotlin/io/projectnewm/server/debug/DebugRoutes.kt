package io.projectnewm.server.debug

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.sessions
import io.projectnewm.server.sessions.token
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h6
import kotlinx.html.p

// Home page for debug & testing during development
fun Routing.createDebugRoutes() {
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
                    a("/v1/portal/songs") { +"Test Get Portal Songs (expect 401 if logged out)" }
                }
                p {
                    a("/mobile") { +"Test Get Mobile Placeholder (expect 401 if logged out)" }
                }
                p {
                    a("/jwt") { +"Test JWT (expect 401 if logged out)" }
                }
                p {
                    a("/v1/users/me") { +"Test Get User Profile (expect 401 if logged out)" }
                }
            }
        }
    }
    get("/login") {
        call.respondHtml {
            body {
                p {
                    a("/login/google") { +"Login with Google" }
                }
                p {
                    a("/login/facebook") { +"Login with Facebook" }
                }
                p {
                    a("/login/linkedin") { +"Login with LinkedIn" }
                }
            }
        }
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
                    p {
                        text("Raw: ${call.sessions.token}")
                    }
                }
            }
        }
    }
}
