package io.projectnewm.server.user

import io.projectnewm.server.user.oauth.providers.FacebookUserProvider
import io.projectnewm.server.user.oauth.providers.GoogleUserProvider
import io.projectnewm.server.user.oauth.providers.LinkedInUserProvider
import io.projectnewm.server.user.repo.UserRepositoryImpl
import org.koin.dsl.module

val userKoinModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get(), get()) }
    single { GoogleUserProvider(get()) }
    single { FacebookUserProvider(get()) }
    single { LinkedInUserProvider(get()) }
}
