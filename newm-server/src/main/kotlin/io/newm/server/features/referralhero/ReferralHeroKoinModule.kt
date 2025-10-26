package io.newm.server.features.referralhero

import io.newm.server.features.referralhero.repo.ReferralHeroRepository
import io.newm.server.features.referralhero.repo.ReferralHeroRepositoryImpl
import org.koin.dsl.module

val referralHeroKoinModule =
    module {
        single<ReferralHeroRepository> { ReferralHeroRepositoryImpl(get(), get(), get(), get(), get()) }
    }
