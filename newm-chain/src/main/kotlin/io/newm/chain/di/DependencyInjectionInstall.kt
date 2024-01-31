package io.newm.chain.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.chain.daemon.daemonsKoinModule
import io.newm.chain.database.databaseKoinModule
import io.newm.chain.grpc.grpcKoinModule
import io.newm.chain.ledger.ledgerKoinModule
import io.newm.shared.ktx.getConfigSplitStrings
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

fun Application.installDependencyInjection() {
    val appKoinModule =
        module {
            single { this@installDependencyInjection }
            factory { get<Application>().environment }
            factory { params -> LoggerFactory.getLogger(params.get<String>()) }
        }

    val monitorAddresses = environment.getConfigSplitStrings("newmchain.monitorAddresses")

    install(Koin) {
        modules(
            appKoinModule,
            daemonsKoinModule(monitorAddresses),
            databaseKoinModule,
            grpcKoinModule,
            ledgerKoinModule,
        )
    }
}
