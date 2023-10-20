package io.newm.server.features.idenfy

import io.ktor.server.application.*
import io.newm.shared.ktx.getConfigString
import io.newm.server.features.idenfy.repo.IdenfyRepository
import io.newm.server.features.idenfy.repo.IdenfyRepositoryImpl
import io.newm.server.ktx.getSecureConfigString
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.security.Key
import javax.crypto.spec.SecretKeySpec

val IDENFY_KEY_QUALIFIER: Qualifier = named("IdenfyKey")

val idenfyKoinModule = module {
    single<Key>(IDENFY_KEY_QUALIFIER) {
        with(get<ApplicationEnvironment>()) {
            SecretKeySpec(
                runBlocking { getSecureConfigString("idenfy.signature.key").toByteArray() },
                getConfigString("idenfy.signature.algorithm")
            )
        }
    }

    single<IdenfyRepository> { IdenfyRepositoryImpl(get(), get(), get(), get()) }
}
