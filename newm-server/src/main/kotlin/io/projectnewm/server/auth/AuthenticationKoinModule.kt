package io.projectnewm.server.auth

import io.projectnewm.server.auth.twofactor.TwoFactorAuthRepository
import io.projectnewm.server.auth.twofactor.repo.TwoFactorAuthRepositoryImpl
import org.koin.dsl.module

val authKoinModule = module {
    single<TwoFactorAuthRepository> { TwoFactorAuthRepositoryImpl(get(), get()) }
}
