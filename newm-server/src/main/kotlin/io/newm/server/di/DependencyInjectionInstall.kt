package io.newm.server.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.server.auth.authKoinModule
import io.newm.server.aws.awsKoinModule
import io.newm.server.client.clientKoinModule
import io.newm.server.config.configKoinModule
import io.newm.server.features.cardano.cardanoKoinModule
import io.newm.server.features.cloudinary.cloudinaryKoinModule
import io.newm.server.features.daemon.daemonsKoinModule
import io.newm.server.features.idenfy.idenfyKoinModule
import io.newm.server.features.playlist.playlistKoinModule
import io.newm.server.features.song.songKoinModule
import io.newm.server.features.user.userKoinModule
import io.newm.server.serialization.serializationModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

fun Application.installDependencyInjection() {
    val appKoinModule = module {
        single { this@installDependencyInjection }
        factory { get<Application>().environment }
        factory { params -> LoggerFactory.getLogger(params.get<String>()) }
    }
    install(Koin) {
        modules(
            appKoinModule,
            configKoinModule,
            serializationModule,
            clientKoinModule,
            userKoinModule,
            authKoinModule,
            songKoinModule,
            playlistKoinModule,
            cloudinaryKoinModule,
            awsKoinModule,
            idenfyKoinModule,
            cardanoKoinModule,
            daemonsKoinModule,
        )
    }
}
