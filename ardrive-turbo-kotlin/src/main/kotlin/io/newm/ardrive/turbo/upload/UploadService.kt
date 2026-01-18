package io.newm.ardrive.turbo.upload

import io.newm.ardrive.turbo.upload.model.AccountBalance
import io.newm.ardrive.turbo.upload.model.CreditShareApproval
import io.newm.ardrive.turbo.upload.model.CreditShareApprovalsResponse
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.TransactionStatus
import io.newm.ardrive.turbo.upload.model.UploadDataItemResponse
import io.newm.ardrive.turbo.upload.model.UploadPrice
import io.newm.ardrive.turbo.upload.model.UploadServiceInfo

/**
 * Upload service endpoints for Turbo data item operations.
 */
interface UploadService {
    suspend fun uploadDataItem(
        dataItem: ByteArray,
        dataItemOptions: DataItemOptions? = null,
        token: String? = null,
    ): UploadDataItemResponse

    suspend fun uploadSignedDataItem(
        signedDataItem: ByteArray,
        dataItemOptions: DataItemOptions? = null,
        token: String? = null,
    ): UploadDataItemResponse

    suspend fun getTransactionStatus(
        transactionId: String,
    ): TransactionStatus

    suspend fun getAccountBalance(
        accountId: String? = null,
    ): AccountBalance

    suspend fun getServiceInfo(): UploadServiceInfo

    suspend fun shareCredits(
        approvedAddress: String,
        approvedWincAmount: String,
        expiresBySeconds: Long? = null,
    ): CreditShareApproval

    suspend fun revokeCredits(
        revokedAddress: String,
    ): List<CreditShareApproval>

    suspend fun listCreditShares(
        userAddress: String? = null,
    ): CreditShareApprovalsResponse

    suspend fun getCreditApprovals(
        userAddress: String? = null,
    ): CreditShareApprovalsResponse
}
