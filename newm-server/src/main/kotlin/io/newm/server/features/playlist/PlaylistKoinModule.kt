package io.newm.server.features.playlist

import io.newm.server.features.playlist.repo.PlaylistRepository
import io.newm.server.features.playlist.repo.PlaylistRepositoryImpl
import org.koin.dsl.module

val playlistKoinModule =
    module {
        single<PlaylistRepository> { PlaylistRepositoryImpl() }
    }
