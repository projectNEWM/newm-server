package io.projectnewm.server.features.user

import io.projectnewm.server.features.user.oauth.providers.FacebookUserProvider
import io.projectnewm.server.features.user.oauth.providers.GoogleUserProvider
import io.projectnewm.server.features.user.oauth.providers.LinkedInUserProvider
import io.projectnewm.server.features.user.repo.UserRepository
import io.projectnewm.server.features.user.repo.UserRepositoryImpl
import org.koin.dsl.module

val userKoinModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get(), get()) }
    single { GoogleUserProvider(get()) }
    single { FacebookUserProvider(get()) }
    single { LinkedInUserProvider(get()) }
}
