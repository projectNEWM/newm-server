package io.newm.server.features.walletconnection

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.walletconnection.repo.WalletConnectionRepository
import io.newm.server.ktx.connectionId
import io.newm.server.ktx.myUserId
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import org.koin.ktor.ext.inject

private const val ROOT_PATH = "v1/wallet-connections"

fun Routing.createWalletConnectionRoutes() {
    val walletConnectionRepository: WalletConnectionRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    route(ROOT_PATH) {
        post("challenges/generate") {
            recaptchaRepository.verify("generate_challenge", request)
            respond(walletConnectionRepository.generateChallenge(receive()))
        }

        post("challenges/answer") {
            recaptchaRepository.verify("answer_challenge", request)
            respond(walletConnectionRepository.answerChallenge(receive()))
        }

        get("{connectionId}/qrcode") {
            recaptchaRepository.verify("qrcode", request)
            respondBytes(ContentType.Image.PNG) {
                walletConnectionRepository.generateQRCode(connectionId)
            }
        }

        authenticate(AUTH_JWT) {
            get {
                respond(walletConnectionRepository.getUserConnections(myUserId))
            }
            route("{connectionId}") {
                get {
                    respond(walletConnectionRepository.connect(connectionId, myUserId))
                }
                delete {
                    walletConnectionRepository.disconnect(connectionId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
