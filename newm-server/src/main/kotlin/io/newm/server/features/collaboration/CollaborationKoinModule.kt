package io.newm.server.features.collaboration

import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.collaboration.repo.CollaborationRepositoryImpl
import org.koin.dsl.module

val collaborationKoinModule = module {
    single<CollaborationRepository> { CollaborationRepositoryImpl(get(), get(), get()) }
}
