package io.newm.server.features.user

import io.newm.server.features.user.oauth.providers.AppleUserProvider
import io.newm.server.features.user.oauth.providers.FacebookUserProvider
import io.newm.server.features.user.oauth.providers.GoogleUserProvider
import io.newm.server.features.user.oauth.providers.LinkedInUserProvider
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.features.user.repo.UserRepositoryImpl
import io.newm.server.features.user.verify.AppleMusicProfileUrlVerifier
import io.newm.server.features.user.verify.OutletProfileUrlVerifier
import io.newm.server.features.user.verify.SoundCloudProfileUrlVerifier
import io.newm.server.features.user.verify.SpotifyProfileUrlVerifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

val userKoinModule = module {
    single<OutletProfileUrlVerifier>(named("spotifyProfileUrlVerifier")) {
        SpotifyProfileUrlVerifier(get(), get())
    }
    single<OutletProfileUrlVerifier>(named("appleMusicProfileUrlVerifier")) {
        AppleMusicProfileUrlVerifier(get(), get())
    }
    single<OutletProfileUrlVerifier>(named("soundCloudProfileUrlVerifier")) {
        SoundCloudProfileUrlVerifier(get(), get())
    }
    single<UserRepository> {
        UserRepositoryImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(named("spotifyProfileUrlVerifier")),
            get(named("appleMusicProfileUrlVerifier")),
            get(named("soundCloudProfileUrlVerifier"))
        )
    }
    single { GoogleUserProvider(get(), get()) }
    single { FacebookUserProvider(get(), get()) }
    single { LinkedInUserProvider(get(), get()) }
    single { AppleUserProvider(get()) }
}
