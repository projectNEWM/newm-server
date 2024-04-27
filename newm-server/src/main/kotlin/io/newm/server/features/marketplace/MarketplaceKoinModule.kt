package io.newm.server.features.marketplace

import io.newm.server.features.marketplace.daemon.MarketplaceMonitorDaemon
import io.newm.server.features.marketplace.repo.MarketplaceRepository
import io.newm.server.features.marketplace.repo.MarketplaceRepositoryImpl
import io.newm.shared.daemon.Daemon
import org.koin.dsl.module

val marketplaceKoinModule =
    module {
        single<Daemon> { MarketplaceMonitorDaemon(get(), get(), get()) }
        single<MarketplaceRepository> { MarketplaceRepositoryImpl(get(), get()) }
    }
