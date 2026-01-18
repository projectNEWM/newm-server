package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class CreditShareApproval(
    val approvalDataItemId: String,
    val approvedAddress: String,
    val payingAddress: String,
    val approvedWincAmount: String,
    val usedWincAmount: String,
    val creationDate: String,
    val expirationDate: String? = null,
)
