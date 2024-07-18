package io.newm.server.features.daemon

import io.newm.server.features.earnings.daemon.MonitorClaimOrderSchedulerDaemon
import io.newm.shared.daemon.Daemon
import org.koin.dsl.bind
import org.koin.dsl.module

private val quartzSchedulerDaemon by lazy { QuartzSchedulerDaemon() }
private val monitorClaimOrderSchedulerDaemon by lazy { MonitorClaimOrderSchedulerDaemon() }

val daemonsKoinModule =
    module {
        single { AwsSqsDaemon() } bind Daemon::class
        single<QuartzSchedulerDaemon> { quartzSchedulerDaemon }
        single { quartzSchedulerDaemon } bind Daemon::class
        single<MonitorClaimOrderSchedulerDaemon> { monitorClaimOrderSchedulerDaemon }
        single { monitorClaimOrderSchedulerDaemon } bind Daemon::class
    }
