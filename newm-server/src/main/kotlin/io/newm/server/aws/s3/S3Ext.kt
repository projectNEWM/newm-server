package io.newm.server.aws.s3

import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.services.s3.AmazonS3
import io.ktor.util.*
import io.newm.chain.util.toB64String
import io.newm.server.aws.s3.model.MapCondition
import io.newm.server.aws.s3.model.PresignedPost
import io.newm.server.aws.s3.model.PresignedPostOptionBuilder
import io.newm.server.aws.s3.model.PresignedPostPolicy
import io.newm.server.aws.s3.model.StartsWithCondition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

private const val ALGORITHM_QUERY_PARAM = "X-Amz-Algorithm"
private const val CREDENTIAL_QUERY_PARAM = "X-Amz-Credential"
private const val AMZ_DATE_QUERY_PARAM = "X-Amz-Date"
private const val TOKEN_QUERY_PARAM = "X-Amz-Security-Token"
private const val SIGNATURE_QUERY_PARAM = "X-Amz-Signature"
private const val ALGORITHM_IDENTIFIER = "AWS4-HMAC-SHA256"
private const val POLICY_QUERY_PARAM = "policy"
private const val KEY_TYPE_IDENTIFIER = "aws4_request"

private val UTC: ZoneId = ZoneId.of("Z")
private val amzTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US).withZone(UTC)

private val amzDateFormattter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US).withZone(UTC)

private val responseDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':'mm':'ss'.'SSS'Z'", Locale.US).withZone(UTC)

private fun computeSignature(encodedPolicy: String, secretKey: String, region: String, date: Instant): String {
    val shortDate = amzDateFormattter.format(date)
    val dateKey = HmacUtils(HmacAlgorithms.HMAC_SHA_256, "AWS4$secretKey").hmac(shortDate)
    val dateRegionKey = HmacUtils(HmacAlgorithms.HMAC_SHA_256, dateKey).hmac(region)
    val dateRegionServiceKey = HmacUtils(HmacAlgorithms.HMAC_SHA_256, dateRegionKey).hmac("s3")
    val signingKey = HmacUtils(HmacAlgorithms.HMAC_SHA_256, dateRegionServiceKey).hmac("aws4_request")
    return HmacUtils(HmacAlgorithms.HMAC_SHA_256, signingKey).hmacHex(encodedPolicy)
}

private inline fun createScope(shortDate: String, region: String, service: String): String =
    "$shortDate/$region/$service/$KEY_TYPE_IDENTIFIER"

/**
 * Creates a presigned post object for uploading a file to S3. The presigned post object contains a URL and a map of
 * fields that should be used in a POST form to upload the file.
 *
 * This is a Kotlin port of the NodeJS
 * [implementation](https://github.com/aws/aws-sdk-js-v3/blob/3d2bea8bbc829c0b05604b9eefca5c84b4e11cbc/packages/s3-presigned-post/src/createPresignedPost.ts)
 * from the AWS SDK for JavaScript.
 **/
fun AmazonS3.createPresignedPost(block: PresignedPostOptionBuilder.() -> Unit): PresignedPost {
    val options = PresignedPostOptionBuilder().apply(block).options()
    val client = this
    val now = Instant.now()

    // signingDate in format like '20201028T070711Z'.
    val signingDate = amzTimeFormatter.format(now)
    val shortDate = amzDateFormattter.format(now)
    val clientRegion = client.region

    // Prepare credentials.
    val credentials = options.credentials
    val credentialScope = createScope(shortDate, clientRegion.firstRegionId, "s3")
    val credential = "${credentials.awsAccessKeyId}/$credentialScope"

    val fields = mutableMapOf<String, String>()
    fields.putAll(options.fields)
    fields.putAll(
        mapOf(
            "bucket" to options.bucket,
            "key" to options.key,
            ALGORITHM_QUERY_PARAM to ALGORITHM_IDENTIFIER,
            CREDENTIAL_QUERY_PARAM to credential,
            AMZ_DATE_QUERY_PARAM to signingDate,
        )
    )
    if (credentials is AWSSessionCredentials) {
        fields[TOKEN_QUERY_PARAM] = credentials.sessionToken
    }

    // Prepare policies.
    val expiration = now.plusSeconds(options.expiresSeconds)
    val conditions = options.conditions.toMutableList()
    fields.forEach { (key, value) ->
        conditions.add(MapCondition(key = key.toLowerCasePreservingASCIIRules(), value = value))
    }
    if (options.key.endsWith("\${filename}")) {
        conditions.add(StartsWithCondition(startsWith = "\$key", value = options.key.substringBefore("\${filename}")))
    } else {
        conditions.add(MapCondition(key = "key", value = options.key))
    }
    val postPolicy = PresignedPostPolicy(
        expiration = responseDateFormatter.format(expiration),
        conditions = conditions
    )
    val policyJson: String = Json.encodeToString(postPolicy)
    val encodedPolicy = policyJson.toByteArray().toB64String()

    // Sign the request.
    val signature = computeSignature(encodedPolicy, credentials.awsSecretKey, clientRegion.firstRegionId, now)
    fields[POLICY_QUERY_PARAM] = encodedPolicy
    fields[SIGNATURE_QUERY_PARAM] = signature

    val url = client.getUrl(options.bucket, options.key)
    return PresignedPost(
        url = "${url.protocol}://${url.host}",
        fields = fields
    )
}
