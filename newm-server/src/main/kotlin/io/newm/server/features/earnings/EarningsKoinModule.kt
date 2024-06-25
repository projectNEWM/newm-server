package io.newm.server.features.earnings

import io.newm.server.features.earnings.daemon.MonitorClaimOrderSchedulerDaemon
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.features.earnings.repo.EarningsRepositoryImpl
import io.newm.shared.daemon.Daemon
import org.koin.dsl.module

private val monitorClaimOrderSchedulerDaemon by lazy { MonitorClaimOrderSchedulerDaemon() }

val earningsKoinModule =
    module {
        single<EarningsRepository> { EarningsRepositoryImpl(get(), get(), get(), get(), get()) }
        single<MonitorClaimOrderSchedulerDaemon> { monitorClaimOrderSchedulerDaemon }
        single<Daemon> { monitorClaimOrderSchedulerDaemon }
    }
