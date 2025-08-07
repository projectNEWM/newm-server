package io.newm.server.features.paypal

import io.newm.server.client.QUALIFIER_PAYPAL_HTTP_CLIENT
import io.newm.server.features.paypal.repo.PayPalRepository
import io.newm.server.features.paypal.repo.PayPalRepositoryImpl
import org.koin.dsl.module

val payPalKoinModule =
    module {
        single<PayPalRepository> { PayPalRepositoryImpl(get(), get(QUALIFIER_PAYPAL_HTTP_CLIENT), get()) }
    }
