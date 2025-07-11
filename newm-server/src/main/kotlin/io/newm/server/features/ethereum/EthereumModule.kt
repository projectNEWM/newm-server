package io.newm.server.features.ethereum

import io.newm.server.features.ethereum.repo.EthereumRepository
import io.newm.server.features.ethereum.repo.EthereumRepositoryImpl
import org.koin.dsl.module

val ethereumKoinModule =
    module {
        single<EthereumRepository> { EthereumRepositoryImpl(get(), get()) }
    }
