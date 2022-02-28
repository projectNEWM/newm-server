package io.projectnewm.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.projectnewm.server.koin.Koin
import io.projectnewm.server.portal.song.songKoinModule
import io.projectnewm.server.user.userKoinModule

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(userKoinModule, songKoinModule)
    }
}
