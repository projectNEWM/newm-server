package io.newm.chain.daemon

import org.koin.dsl.bind
import org.koin.dsl.module

val daemonsKoinModule = module {
    single { BlockDaemon(get()) } bind Daemon::class
}