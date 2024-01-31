package io.newm.server.features.minting

import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.minting.repo.MintingRepositoryImpl
import org.koin.dsl.module

val mintingKoinModule =
    module {
        single<MintingRepository> { MintingRepositoryImpl(get(), get(), get(), get()) }
    }
