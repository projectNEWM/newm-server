package io.newm.chain.daemon

import io.newm.shared.daemon.Daemon
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun daemonsKoinModule(monitorAddresses: List<String>) =
    module {
        single {
            BlockDaemon(
                get(),
                get(),
                get(),
                get(),
                get(named("confirmedBlockFlow")),
            )
        } bind Daemon::class

        monitorAddresses.distinct().forEach { address ->
            single(named(address)) { MonitorAddressDaemon(get(), get(), get(), address) } bind Daemon::class
        }
    }
