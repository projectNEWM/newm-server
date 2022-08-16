package io.newm.chain.di

import io.ktor.server.application.*
import io.newm.chain.daemon.daemonsKoinModule
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
            daemonsKoinModule,
        )
    }
}
