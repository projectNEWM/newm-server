package io.newm.server.features.dripdropz

import io.newm.server.features.dripdropz.repo.DripDropzRepository
import io.newm.server.features.dripdropz.repo.DripDropzRepositoryImpl
import org.koin.dsl.module

val dripDropzKoinModule =
    module {
        single<DripDropzRepository> { DripDropzRepositoryImpl(get(), get()) }
    }
