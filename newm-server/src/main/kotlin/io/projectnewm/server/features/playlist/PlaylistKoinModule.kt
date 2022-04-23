package io.projectnewm.server.features.playlist

import io.projectnewm.server.features.playlist.repo.PlaylistRepository
import io.projectnewm.server.features.playlist.repo.PlaylistRepositoryImpl
import org.koin.dsl.module

val playlistKoinModule = module {
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
}
