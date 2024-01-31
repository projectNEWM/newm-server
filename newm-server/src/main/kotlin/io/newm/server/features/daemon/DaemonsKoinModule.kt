package io.newm.server.features.daemon

import io.newm.shared.daemon.Daemon
import org.koin.dsl.bind
import org.koin.dsl.module

private val quartzSchedulerDaemon by lazy { QuartzSchedulerDaemon() }

val daemonsKoinModule =
    module {
        single { AwsSqsDaemon() } bind Daemon::class
        single<QuartzSchedulerDaemon> { quartzSchedulerDaemon }
        single { quartzSchedulerDaemon } bind Daemon::class
    }
