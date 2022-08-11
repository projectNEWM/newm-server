package io.newm.server.auth

import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.auth.jwt.repo.JwtRepositoryImpl
import io.newm.server.auth.oauth.repo.OAuthRepository
import io.newm.server.auth.oauth.repo.OAuthRepositoryImpl
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepository
import io.newm.server.auth.twofactor.repo.TwoFactorAuthRepositoryImpl
import org.koin.dsl.module

val authKoinModule = module {
    single<TwoFactorAuthRepository> { TwoFactorAuthRepositoryImpl(get(), get()) }
    single<OAuthRepository> { OAuthRepositoryImpl(get(), get(), get()) }
    single<JwtRepository> { JwtRepositoryImpl(get(), get()) }
}
