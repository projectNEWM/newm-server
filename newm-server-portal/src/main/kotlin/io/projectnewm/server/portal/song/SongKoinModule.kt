package io.projectnewm.server.portal.song

import io.projectnewm.server.portal.song.impl.SongRepositoryImpl
import org.koin.dsl.module

val songKoinModule = module {
    single<SongRepository> { SongRepositoryImpl() }
}
