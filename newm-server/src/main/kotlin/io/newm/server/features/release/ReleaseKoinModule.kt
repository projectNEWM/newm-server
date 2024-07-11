package io.newm.server.features.release

import io.newm.server.client.QUALIFIER_SPOTIFY_HTTP_CLIENT
import io.newm.server.features.release.repo.OutletReleaseRepository
import io.newm.server.features.release.repo.OutletReleaseRepositoryImpl
import org.koin.dsl.module

val releaseKoinModule =
    module {
        single<OutletReleaseRepository> { OutletReleaseRepositoryImpl(get(QUALIFIER_SPOTIFY_HTTP_CLIENT), get(), get()) }
    }
