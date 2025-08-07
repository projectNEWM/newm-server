package io.newm.server.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.server.auth.authKoinModule
import io.newm.server.aws.awsKoinModule
import io.newm.server.client.clientKoinModule
import io.newm.server.config.configKoinModule
import io.newm.server.curator.curatorKoinModule
import io.newm.server.database.databaseKoinModule
import io.newm.server.features.arweave.arweaveKoinModule
import io.newm.server.features.cardano.cardanoKoinModule
import io.newm.server.features.cloudinary.cloudinaryKoinModule
import io.newm.server.features.collaboration.collaborationKoinModule
import io.newm.server.features.daemon.daemonsKoinModule
import io.newm.server.features.distribution.distributionKoinModule
import io.newm.server.features.dripdropz.dripDropzKoinModule
import io.newm.server.features.earnings.earningsKoinModule
import io.newm.server.features.email.emailKoinModule
import io.newm.server.features.ethereum.ethereumKoinModule
import io.newm.server.features.idenfy.idenfyKoinModule
import io.newm.server.features.marketplace.marketplaceKoinModule
import io.newm.server.features.minting.mintingKoinModule
import io.newm.server.features.nftcdn.nftCdnKoinModule
import io.newm.server.features.paypal.payPalKoinModule
import io.newm.server.features.playlist.playlistKoinModule
import io.newm.server.features.referralhero.referralHeroKoinModule
import io.newm.server.features.release.releaseKoinModule
import io.newm.server.features.song.songKoinModule
import io.newm.server.features.user.userKoinModule
import io.newm.server.features.walletconnection.walletConnectionKoinModule
import io.newm.server.recaptcha.recaptchaKoinModule
import io.newm.server.serialization.serializationModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

fun Application.installDependencyInjection() {
    val appKoinModule =
        module {
            single { this@installDependencyInjection }
            factory { get<Application>().environment }
            factory { params -> LoggerFactory.getLogger(params.get<String>()) }
        }
    install(Koin) {
        modules(
            appKoinModule,
            databaseKoinModule,
            curatorKoinModule,
            configKoinModule,
            serializationModule,
            clientKoinModule,
            userKoinModule,
            authKoinModule,
            recaptchaKoinModule,
            emailKoinModule,
            songKoinModule,
            collaborationKoinModule,
            playlistKoinModule,
            cloudinaryKoinModule,
            awsKoinModule,
            idenfyKoinModule,
            cardanoKoinModule,
            ethereumKoinModule,
            daemonsKoinModule,
            distributionKoinModule,
            arweaveKoinModule,
            mintingKoinModule,
            releaseKoinModule,
            earningsKoinModule,
            walletConnectionKoinModule,
            marketplaceKoinModule,
            nftCdnKoinModule,
            dripDropzKoinModule,
            referralHeroKoinModule,
            payPalKoinModule,
        )
    }
}
