package io.newm.server.features.user

import io.newm.server.client.QUALIFIER_APPLE_MUSIC_HTTP_CLIENT
import io.newm.server.client.QUALIFIER_SOUND_CLOUD_HTTP_CLIENT
import io.newm.server.client.QUALIFIER_SPOTIFY_HTTP_CLIENT
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

val QUALIFIER_SPOTIFY_PROFILE_URL_VERIFIER = named("spotifyProfileUrlVerifier")
val QUALIFIER_APPLE_MUSIC_PROFILE_URL_VERIFIER = named("appleMusicProfileUrlVerifier")
val QUALIFIER_SOUND_CLOUD_PROFILE_URL_VERIFIER = named("soundCloudProfileUrlVerifier")

val userKoinModule =
    module {
        single<OutletProfileUrlVerifier>(QUALIFIER_SPOTIFY_PROFILE_URL_VERIFIER) {
            SpotifyProfileUrlVerifier(get(QUALIFIER_SPOTIFY_HTTP_CLIENT))
        }
        single<OutletProfileUrlVerifier>(QUALIFIER_APPLE_MUSIC_PROFILE_URL_VERIFIER) {
            AppleMusicProfileUrlVerifier(get(QUALIFIER_APPLE_MUSIC_HTTP_CLIENT))
        }
        single<OutletProfileUrlVerifier>(QUALIFIER_SOUND_CLOUD_PROFILE_URL_VERIFIER) {
            SoundCloudProfileUrlVerifier(get(QUALIFIER_SOUND_CLOUD_HTTP_CLIENT))
        }
        single<UserRepository> {
            UserRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(QUALIFIER_SPOTIFY_PROFILE_URL_VERIFIER),
                get(QUALIFIER_APPLE_MUSIC_PROFILE_URL_VERIFIER),
                get(QUALIFIER_SOUND_CLOUD_PROFILE_URL_VERIFIER),
                get(),
                get()
            )
        }
        single { GoogleUserProvider(get(), get()) }
        single { FacebookUserProvider(get(), get()) }
        single { LinkedInUserProvider(get(), get()) }
        single { AppleUserProvider(get()) }
    }
