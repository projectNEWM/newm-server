package io.newm.server.features.earnings

import io.newm.server.features.earnings.repo.EarningsRepositoryImpl
import org.koin.dsl.module

val earningsKoinModule =
    module {
        single { EarningsRepositoryImpl(get(), get(), get()) }
    }
