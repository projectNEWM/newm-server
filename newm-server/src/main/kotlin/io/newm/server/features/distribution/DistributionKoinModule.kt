package io.newm.server.features.distribution

import io.newm.server.features.distribution.eveara.EvearaDistributionRepositoryImpl
import org.koin.dsl.module

val distributionKoinModule = module {
    single<DistributionRepository> { EvearaDistributionRepositoryImpl(get(), get()) }
}
