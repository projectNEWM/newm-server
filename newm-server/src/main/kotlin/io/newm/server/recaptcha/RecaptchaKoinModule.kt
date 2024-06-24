package io.newm.server.recaptcha

import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.server.recaptcha.repo.RecaptchaRepositoryImpl
import org.koin.dsl.module

val recaptchaKoinModule =
    module {
        single<RecaptchaRepository> { RecaptchaRepositoryImpl(get(), get(), get(), get()) }
    }
