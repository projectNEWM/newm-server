package io.projectnewm.server.features.song

import io.projectnewm.server.features.song.repo.SongRepository
import io.projectnewm.server.features.song.repo.SongRepositoryImpl
import org.koin.dsl.module

val songKoinModule = module {
    single<SongRepository> { SongRepositoryImpl(get()) }
}
