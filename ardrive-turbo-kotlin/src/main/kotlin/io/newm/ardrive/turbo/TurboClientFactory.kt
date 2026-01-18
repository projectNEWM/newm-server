package io.newm.ardrive.turbo

import io.ktor.client.HttpClient
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.payment.TokenTools

/**
 * Factory helpers for creating authenticated and unauthenticated Turbo clients.
 */
object TurboClientFactory {
    /**
     * Creates a Turbo client that signs requests with the provided wallet JSON.
     */
    fun createAuthenticated(
        walletJson: String,
        config: TurboConfig,
        tokenTools: TokenTools? = null,
        httpClient: HttpClient? = null,
    ): TurboClient =
        TurboClientImpl(
            config = config,
            signer = ArweaveSigner(walletJson),
            tokenTools = tokenTools,
            httpClient = httpClient,
        )

    /**
     * Creates a Turbo client that can access public payment endpoints.
     */
    fun createUnauthenticated(
        config: TurboConfig,
        httpClient: HttpClient? = null,
    ): TurboClientUnauthenticated =
        TurboClientUnauthenticatedImpl(
            config = config,
            httpClient = httpClient,
        )
}
