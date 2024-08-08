package io.newm.server.aws.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

fun s3UrlStringOf(
    bucketName: String,
    key: String
): String = "s3://$bucketName/$key"

fun S3Client.doesObjectExist(
    bucketName: String,
    key: String
): Boolean =
    try {
        val request = HeadObjectRequest
            .builder()
            .bucket(bucketName)
            .key(key)
            .build()
        headObject(request)
        true
    } catch (e: NoSuchKeyException) {
        false
    }

fun S3Client.doesBucketExist(bucketName: String): Boolean =
    try {
        val request = HeadBucketRequest
            .builder()
            .bucket(bucketName)
            .build()
        headBucket(request)
        true
    } catch (e: NoSuchBucketException) {
        false
    }
