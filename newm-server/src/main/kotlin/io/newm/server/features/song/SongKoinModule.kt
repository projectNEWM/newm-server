package io.newm.server.features.song

import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.song.repo.SongRepositoryImpl
import org.koin.dsl.module

val songKoinModule = module {
    single<SongRepository> { SongRepositoryImpl(get(), get(), get(), get()) }
}
