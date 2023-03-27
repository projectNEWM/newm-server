package io.newm.chain.database

import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.ChainRepositoryImpl
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.repository.LedgerRepositoryImpl
import io.newm.chain.database.repository.UsersRepository
import io.newm.chain.database.repository.UsersRepositoryImpl
import org.koin.dsl.module

val databaseKoinModule = module {
    single<ChainRepository> { ChainRepositoryImpl() }
    single<LedgerRepository> { LedgerRepositoryImpl() }
    single<UsersRepository> { UsersRepositoryImpl() }
}
