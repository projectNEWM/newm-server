package io.projectnewm.server.koin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.projectnewm.server.client.clientKoinModule
import io.projectnewm.server.portal.song.songKoinModule
import io.projectnewm.server.user.userKoinModule
import org.koin.dsl.module

fun Application.installDependencyInjection() {
    val appKoinModule = module {
        single { this@installDependencyInjection }
        factory { get<Application>().environment }
        factory { get<Application>().log }
    }
    install(Koin) {
        modules(appKoinModule, clientKoinModule, userKoinModule, songKoinModule)
    }
}
