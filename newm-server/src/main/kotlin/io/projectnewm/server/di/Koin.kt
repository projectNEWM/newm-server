package io.projectnewm.server.di

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.util.AttributeKey
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration

// Workaround for https://github.com/InsertKoinIO/koin/issues/1295
class Koin {
    companion object Plugin : ApplicationPlugin<Application, KoinApplication, Koin> {
        override val key = AttributeKey<Koin>("Koin")
        override fun install(pipeline: Application, configure: KoinAppDeclaration): Koin {
            pipeline.environment.monitor.subscribe(ApplicationStopping) {
                stopKoin()
            }
            startKoin(configure)
            return Koin()
        }
    }
}
