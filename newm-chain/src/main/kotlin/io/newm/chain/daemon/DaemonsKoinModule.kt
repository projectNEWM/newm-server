package io.newm.chain.daemon

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val daemonsKoinModule = module {
    single {
        BlockDaemon(
            get(),
            get(),
            get(),
            get(),
            get(named("blockFlow")),
        )
    } bind Daemon::class
}
