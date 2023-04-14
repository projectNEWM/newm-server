package io.newm.server.features.arweave

import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.server.features.arweave.repo.ArweaveRepositoryImpl
import org.koin.dsl.module

val arweaveKoinModule = module {
    single<ArweaveRepository> { ArweaveRepositoryImpl(get()) }
}
