package io.projectnewm.server.user

import io.projectnewm.server.user.impl.UserRepositoryImpl
import org.koin.dsl.module

val userKoinModule = module {
    single<UserRepository> { UserRepositoryImpl() }
}
