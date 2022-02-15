package io.projectnewm.server.pugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.html.respondHtml
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.projectnewm.server.pugins.auth.google.configureJwtGoogle
import io.projectnewm.server.pugins.auth.google.configureOAuthGoogle
import io.projectnewm.server.pugins.auth.google.routeOAuthGoogle
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.h6
import kotlinx.html.p

fun Application.configureAuthentication() {
    install(Authentication) {
        configureOAuthGoogle(environment)
        configureJwtGoogle(environment)
    }
    routing {
        routeOAuthGoogle()

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
                        a("/login-facebook") { +"Login with Facebook (coming soon)" }
                    }
                    p {
                        a("/login-linkedin") { +"Login with LinkedIn (coming soon)" }
                    }
                }
            }
        }
        get("/logout") {
            call.response.cookies.appendExpired("token")
            call.respondText("Successfully logged out!!!")
        }
    }
}
