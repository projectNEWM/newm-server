package io.newm.server.features.walletconnection

import io.newm.server.features.walletconnection.repo.WalletConnectionRepository
import io.newm.server.features.walletconnection.repo.WalletConnectionRepositoryImpl
import org.koin.dsl.module

val walletConnectionKoinModule =
    module {
        single<WalletConnectionRepository> {
            WalletConnectionRepositoryImpl(get(), get())
        }
    }
