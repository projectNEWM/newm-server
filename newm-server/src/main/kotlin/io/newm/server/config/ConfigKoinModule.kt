package io.newm.server.config

import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepositoryImpl
import org.koin.dsl.module

val configKoinModule = module {
    single<ConfigRepository> { ConfigRepositoryImpl() }
}
