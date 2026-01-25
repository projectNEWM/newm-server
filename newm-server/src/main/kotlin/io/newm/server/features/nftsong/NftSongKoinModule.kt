package io.newm.server.features.nftsong

import io.newm.server.features.nftsong.repo.NftSongRepository
import io.newm.server.features.nftsong.repo.NftSongRepositoryImpl
import org.koin.dsl.module

val nftSongKoinModule =
    module {
        single<NftSongRepository> { NftSongRepositoryImpl(get(), get()) }
    }
