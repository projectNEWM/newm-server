package io.newm.server.aws.s3

fun s3UrlStringOf(bucketName: String, key: String): String = "s3://$bucketName/$key"
