package io.projectnewm.server.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.projectnewm.server.auth.authKoinModule
import io.projectnewm.server.client.clientKoinModule
import io.projectnewm.server.features.playlist.playlistKoinModule
import io.projectnewm.server.features.song.songKoinModule
import io.projectnewm.server.features.user.userKoinModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.installDependencyInjection() {
    val appKoinModule = module {
        single { this@installDependencyInjection }
        factory { get<Application>().environment }
        factory { get<Application>().log }
    }
    install(Koin) {
        modules(
            appKoinModule,
            clientKoinModule,
            userKoinModule,
            authKoinModule,
            songKoinModule,
            playlistKoinModule
        )
    }
}
