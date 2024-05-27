package io.newm.server.features.nftcdn

import io.newm.server.features.nftcdn.repo.NftCdnRepository
import io.newm.server.features.nftcdn.repo.NftCdnRepositoryImpl
import org.koin.dsl.module

val nftCdnKoinModule =
    module {
        single<NftCdnRepository> { NftCdnRepositoryImpl(get()) }
    }
