package io.newm.server.aws.cloudfront

import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import io.newm.server.features.song.model.AudioStreamData
import io.newm.server.security.PrivateKeyReader
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest
import java.time.Instant
import java.time.temporal.ChronoUnit

class CloudfrontAudioStreamArguments(
    var url: String,
    var keyPairId: String,
    var privateKey: String,
    var expirationDate: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
)

class CloudfrontAudioStreamData(private val args: CloudfrontAudioStreamArguments) :
    AudioStreamData {

    override val url: String
        get() = this.args.url
    private val _cookies: Map<String, String>
    override val cookies: Map<String, String>
        get() = this._cookies

    init {
        this._cookies = createSignedCookies()
    }

    private fun createSignedCookies(): Map<String, String> {
        val streamUrl = URLBuilder(this.url).build()

        // resource is the "dirname" of the URL with the filename removed,
        // effectively granting access to the entire "directory"
        val resourceUrl = "${streamUrl.protocol}://${streamUrl.host}${
            streamUrl.fullPath.subSequence(
                0,
                streamUrl.fullPath.lastIndexOf("/")
            )
        }"

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
