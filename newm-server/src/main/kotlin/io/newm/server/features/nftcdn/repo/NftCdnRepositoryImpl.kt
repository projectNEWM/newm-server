package io.newm.server.features.nftcdn.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.core.toByteArray
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.coLazy
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.getOrSet

// Reference: https://github.com/nftcdn/support.nftcdn.io

internal class NftCdnRepositoryImpl(environment: ApplicationEnvironment) : NftCdnRepository {
    private val subdomain: String by coLazy {
        environment.getSecureConfigString("nftCdn.subdomain")
    }

    private val secretKey: ByteArray by coLazy {
        Base64.getDecoder().decode(environment.getSecureConfigString("nftCdn.secretKey"))
    }

    private val macContainer = ThreadLocal<Mac>()

    override fun generateImageUrl(fingerprint: String): String = generateUrl(fingerprint, "image")

    override fun generateFileUrl(
        fingerprint: String,
        index: Int
    ): String = generateUrl(fingerprint, "files/$index")

    override fun generateUrl(
        fingerprint: String,
        path: String,
    ): String {
        val url = urlOf(fingerprint, path, "")
        val mac =
            macContainer.getOrSet {
                Mac.getInstance("HmacSHA256").apply {
                    init(SecretKeySpec(secretKey, "HmacSHA256"))
                }
            }
        val token = Base64.getUrlEncoder().encodeToString(mac.doFinal(url.toByteArray())).trimEnd('=')
        return urlOf(fingerprint, path, token)
    }

    private fun urlOf(
        fingerprint: String,
        path: String,
        token: String
    ): String = "https://$fingerprint.$subdomain.nftcdn.io/$path?tk=$token"
}
