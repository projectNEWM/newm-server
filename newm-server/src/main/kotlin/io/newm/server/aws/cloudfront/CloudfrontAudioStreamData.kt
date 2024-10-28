package io.newm.server.aws.cloudfront

import io.ktor.http.Cookie
import io.newm.server.features.song.model.AudioStreamData
import io.newm.server.security.PrivateKeyReader
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities
import software.amazon.awssdk.services.cloudfront.internal.url.DefaultSignedUrl
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit

data class CloudfrontAudioStreamArguments(
    var url: String,
    var keyPairId: String,
    var privateKey: String,
    var cookieDomain: String,
    var expirationDate: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
)

class CloudfrontAudioStreamData(
    private val args: CloudfrontAudioStreamArguments
) : AudioStreamData {
    override val url: String
        get() = createSignedUrl(this.args.url)
    override val cookies: List<Cookie> by lazy { createSignedCookies() }

    private fun createSignedCookies(): List<Cookie> {
        val streamUrl = this.args.url

        // resource is the "dirname" of the URL with the filename removed,
        // effectively granting access to the entire "directory"
        val resourceUrl = "${streamUrl.removeRange(streamUrl.lastIndexOf("/"), streamUrl.length)}/*"

        val customRequest =
            CustomSignerRequest
                .builder()
                .resourceUrl(resourceUrl)
                .privateKey(PrivateKeyReader.readFromString(args.privateKey))
                .keyPairId(args.keyPairId)
                .expirationDate(args.expirationDate)
                // optional
                //  .activeDate(activeDate)
                //  .ipRange("192.168.0.1/24")
                .build()

        val cloudFrontUtilities = CloudFrontUtilities.create()
        val signedCookies = cloudFrontUtilities.getCookiesForCustomPolicy(customRequest)
        val (signatureHeaderName, signatureHeaderValue) = signedCookies.signatureHeaderValue().split("=")
        val (keyPairIdHeaderName, keyPairIdHeaderValue) = signedCookies.keyPairIdHeaderValue().split("=")
        val (policyHeaderName, policyHeaderValue) = signedCookies.policyHeaderValue().split("=")

        return listOf(
            cookie(name = signatureHeaderName, value = signatureHeaderValue),
            cookie(name = keyPairIdHeaderName, value = keyPairIdHeaderValue),
            cookie(name = policyHeaderName, value = policyHeaderValue),
        )
    }

    private fun createSignedUrl(streamUrl: String): String {
        // resource is the "dirname" of the URL with the filename removed,
        // effectively granting access to the entire "directory"
        val resourceUrl = "${streamUrl.removeRange(streamUrl.lastIndexOf("/"), streamUrl.length)}/*"
        val request =
            CustomSignerRequest
                .builder()
                .resourceUrl(resourceUrl)
                .privateKey(PrivateKeyReader.readFromString(args.privateKey))
                .keyPairId(args.keyPairId)
                .expirationDate(args.expirationDate)
                // optional
                //  .activeDate(activeDate)
                //  .ipRange("192.168.0.1/24")
                .build()

        val policy =
            SigningUtils.buildCustomPolicyForSignedUrl(
                request.resourceUrl(),
                request.activeDate(),
                request.expirationDate(),
                request.ipRange()
            )
        val signatureBytes =
            SigningUtils.signWithSha1Rsa(policy.toByteArray(StandardCharsets.UTF_8), request.privateKey())
        val urlSafePolicy = SigningUtils.makeStringUrlSafe(policy)
        val urlSafeSignature = SigningUtils.makeBytesUrlSafe(signatureBytes)
        val uri = URI.create(streamUrl)
        val protocol = uri.scheme
        val domain = uri.host
        val encodedPath = (
            uri.rawPath +
                (if (uri.getQuery() != null) "?" + uri.rawQuery + "&" else "?") +
                "Policy=" + urlSafePolicy +
                "&oPolicy=" + urlSafePolicy +
                "&Signature=" + urlSafeSignature +
                "&oSignature=" + urlSafeSignature +
                "&Key-Pair-Id=" + request.keyPairId() +
                "&oKey-Pair-Id=" + request.keyPairId()
        )
        val signedUrl =
            DefaultSignedUrl
                .builder()
                .protocol(protocol)
                .domain(domain)
                .encodedPath(encodedPath)
                .url("$protocol://$domain$encodedPath")
                .build()
        return signedUrl.url()
    }

    private fun cookie(
        name: String,
        value: String
    ): Cookie =
        Cookie(
            name = name,
            value = value,
            path = "/",
            domain = args.cookieDomain,
            extensions = mapOf("SameSite" to "Lax"),
            secure = true,
            httpOnly = true,
        )
}

fun cloudfrontAudioStreamData(init: CloudfrontAudioStreamArguments.() -> Unit): CloudfrontAudioStreamData = CloudfrontAudioStreamData(CloudfrontAudioStreamArguments("", "", "", "").apply(init))
