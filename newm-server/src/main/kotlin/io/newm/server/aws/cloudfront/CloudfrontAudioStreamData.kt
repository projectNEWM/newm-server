package io.newm.server.aws.cloudfront

import io.newm.server.features.song.model.AudioStreamData
import io.newm.server.security.PrivateKeyReader
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest
import java.time.Instant
import java.time.temporal.ChronoUnit

data class CloudfrontAudioStreamArguments(
    var url: String,
    var keyPairId: String,
    var privateKey: String,
    var expirationDate: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
)

class CloudfrontAudioStreamData(
    private val args: CloudfrontAudioStreamArguments
) : AudioStreamData {
    override val url: String
        get() = this.args.url
    override val cookies: Map<String, String> by lazy { createSignedCookies() }

    private fun createSignedCookies(): Map<String, String> {
        val streamUrl = this.url

        // resource is the "dirname" of the URL with the filename removed,
        // effectively granting access to the entire "directory"
        val resourceUrl = streamUrl.removeRange(streamUrl.lastIndexOf("/"), streamUrl.length)

        val customRequest = CustomSignerRequest.builder()
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

        return mapOf(
            signatureHeaderName to signatureHeaderValue,
            keyPairIdHeaderName to keyPairIdHeaderValue,
            policyHeaderName to policyHeaderValue
        )
    }
}

fun cloudfrontAudioStreamData(init: CloudfrontAudioStreamArguments.() -> Unit): CloudfrontAudioStreamData =
    CloudfrontAudioStreamData(CloudfrontAudioStreamArguments("", "", "").apply(init))
