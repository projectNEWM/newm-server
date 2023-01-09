package io.newm.chain.database

import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.ChainRepositoryImpl
import io.newm.chain.database.repository.KeysRepository
import io.newm.chain.database.repository.KeysRepositoryImpl
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.repository.LedgerRepositoryImpl
import org.koin.dsl.module

val databaseKoinModule = module {
    single<ChainRepository> { ChainRepositoryImpl() }
    single<KeysRepository> { KeysRepositoryImpl() }
    single<LedgerRepository> { LedgerRepositoryImpl() }
}