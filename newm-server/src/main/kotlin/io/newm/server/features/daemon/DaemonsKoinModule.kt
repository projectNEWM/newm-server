package io.newm.server.features.daemon

import io.newm.shared.daemon.Daemon
import org.koin.dsl.bind
import org.koin.dsl.module

val daemonsKoinModule = module {
    single { AwsSqsDaemon() } bind Daemon::class
    single { QuartzSchedulerDaemon() } bind Daemon::class
}
