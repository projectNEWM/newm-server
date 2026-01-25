package io.newm.server.features.nftsong

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.nftsong.repo.NftSongRepository
import io.newm.server.ktx.myUserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get

fun Routing.createNftSongRoutes() {
    val repository: NftSongRepository by inject()

    authenticate(AUTH_JWT) {
        get("/v1/nft/songs") {
            respond(repository.getNftSongs(myUserId))
        }
    }
}
