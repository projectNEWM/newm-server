package io.newm.chain.ledger

import org.koin.dsl.bind
import org.koin.dsl.module

val ledgerKoinModule = module {
    single { SubmittedTransactionCacheImpl() } bind SubmittedTransactionCache::class
}
