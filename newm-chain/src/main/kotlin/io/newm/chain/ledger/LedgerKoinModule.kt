package io.newm.chain.ledger

import io.newm.kogmios.protocols.model.Block
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val ledgerKoinModule = module {
    single { SubmittedTransactionCacheImpl() } bind SubmittedTransactionCache::class
    single(qualifier = named("blockFlow")) { MutableSharedFlow<Block>(extraBufferCapacity = 100) }
}
