package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadDataItemResponse(
    val dataCaches: List<String> = emptyList(),
    val fastFinalityIndexes: List<String> = emptyList(),
    val id: String,
    val owner: String,
    val winc: String,
    val createdApproval: CreditShareApproval? = null,
    val revokedApprovals: List<CreditShareApproval>? = null,
    val cryptoFundResult: CryptoFundResponse? = null,
)
