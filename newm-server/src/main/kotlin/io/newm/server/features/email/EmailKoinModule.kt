package io.newm.server.features.email

import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.email.repo.EmailRepositoryImpl
import org.koin.dsl.module

val emailKoinModule = module {
    single<EmailRepository> { EmailRepositoryImpl(get()) }
}
