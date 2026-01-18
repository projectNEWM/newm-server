package io.newm.ardrive.turbo.upload

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.AccountBalance
import io.newm.ardrive.turbo.upload.model.CreditShareApproval
import io.newm.ardrive.turbo.upload.model.CreditShareApprovalsResponse
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import io.newm.ardrive.turbo.upload.model.TransactionStatus
import io.newm.ardrive.turbo.upload.model.UploadDataItemResponse
import io.newm.ardrive.turbo.upload.model.UploadServiceInfo
import io.newm.ardrive.turbo.upload.util.CreditSharingTags
import io.newm.ardrive.turbo.upload.util.DataItemSigner
import io.newm.ardrive.turbo.util.HttpClientFactory
import io.newm.ardrive.turbo.util.TurboHttpException
import io.newm.ardrive.turbo.util.toBase64Url

class UploadServiceImpl(
    private val config: TurboConfig,
    private val signer: ArweaveSigner,
    private val httpClient: HttpClient = HttpClientFactory.create(config),
) : UploadService {
    private val dataItemSigner = DataItemSigner(signer)

    override suspend fun uploadDataItem(
        dataItem: ByteArray,
        dataItemOptions: DataItemOptions?,
        token: String?,
    ): UploadDataItemResponse {
        val options = dataItemOptions ?: DataItemOptions()
        val signedDataItem = dataItemSigner.sign(dataItem, options)
        return uploadSignedDataItem(signedDataItem.bytes, options, token)
    }

    override suspend fun uploadSignedDataItem(
        signedDataItem: ByteArray,
        dataItemOptions: DataItemOptions?,
        token: String?,
    ): UploadDataItemResponse {
        val options = dataItemOptions ?: DataItemOptions()
        val signatureHeaders = signer.generateSignatureHeaders().asHeaderMap()
        val response = httpClient
            .post("${config.uploadBaseUrl}/v1/tx/${token ?: ""}") {
                signatureHeaders.forEach { (key, value) -> header(key, value) }
                header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                setBody(ByteArrayContent(signedDataItem))
                options.paidBy.takeIf { it.isNotEmpty() }?.let { paidBy ->
                    header("x-paid-by", paidBy.joinToString(","))
                }
            }.body<UploadDataItemResponse>()
        return response
    }

    override suspend fun getTransactionStatus(transactionId: String): TransactionStatus = httpClient.get("${config.uploadBaseUrl}/v1/tx/$transactionId/status").body()

    override suspend fun getAccountBalance(accountId: String?): AccountBalance {
        val id = accountId ?: toBase64Url(signer.publicKeyModulus)
        return httpClient.get("${config.uploadBaseUrl}/v1/account/balance/$id").body()
    }

    override suspend fun getServiceInfo(): UploadServiceInfo = httpClient.get("${config.uploadBaseUrl}/v1/info").body()

    override suspend fun shareCredits(
        approvedAddress: String,
        approvedWincAmount: String,
        expiresBySeconds: Long?,
    ): CreditShareApproval {
        require(approvedAddress.isNotBlank()) { "approvedAddress is required" }
        require(approvedWincAmount.toDoubleOrNull()?.let { it > 0 } == true) {
            "approvedWincAmount must be a positive number"
        }
        if (expiresBySeconds != null) {
            require(expiresBySeconds > 0) { "expiresBySeconds must be positive" }
        }

        val tags = mutableListOf(
            DataItemTag(CreditSharingTags.SHARE_CREDITS, approvedAddress),
            DataItemTag(CreditSharingTags.SHARED_WINC_AMOUNT, approvedWincAmount),
        )
        if (expiresBySeconds != null) {
            tags += DataItemTag(CreditSharingTags.APPROVAL_EXPIRES_BY_SECONDS, expiresBySeconds.toString())
        }
        val noncePayload = (approvedAddress + approvedWincAmount + System.currentTimeMillis()).toByteArray()
        val response = uploadDataItem(
            dataItem = noncePayload,
            dataItemOptions = DataItemOptions(tags = tags),
            token = null,
        )
        return response.createdApproval
            ?: error("Failed to create credit share approval but upload succeeded")
    }

    override suspend fun revokeCredits(revokedAddress: String): List<CreditShareApproval> {
        require(revokedAddress.isNotBlank()) { "revokedAddress is required" }
        val tags = listOf(
            DataItemTag(CreditSharingTags.REVOKE_CREDITS, revokedAddress),
        )
        val noncePayload = (revokedAddress + System.currentTimeMillis()).toByteArray()
        val response = uploadDataItem(
            dataItem = noncePayload,
            dataItemOptions = DataItemOptions(tags = tags),
            token = null,
        )
        return response.revokedApprovals
            ?: error("Failed to revoke credit share approvals but upload succeeded")
    }

    override suspend fun listCreditShares(userAddress: String?): CreditShareApprovalsResponse {
        val address = userAddress ?: toBase64Url(signer.publicKeyModulus)
        return try {
            httpClient
                .get("${config.uploadBaseUrl}/v1/account/approvals/all?userAddress=$address")
                .body()
        } catch (exception: TurboHttpException) {
            if (exception.status == HttpStatusCode.NotFound) {
                CreditShareApprovalsResponse()
            } else {
                throw exception
            }
        }
    }

    override suspend fun getCreditApprovals(userAddress: String?): CreditShareApprovalsResponse {
        val address = userAddress ?: toBase64Url(signer.publicKeyModulus)
        return try {
            httpClient
                .get("${config.uploadBaseUrl}/v1/account/approvals/get?userAddress=$address")
                .body()
        } catch (exception: TurboHttpException) {
            if (exception.status == HttpStatusCode.NotFound) {
                CreditShareApprovalsResponse()
            } else {
                throw exception
            }
        }
    }
}
