package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class CreditShareApprovalsResponse(
    val givenApprovals: List<CreditShareApproval> = emptyList(),
    val receivedApprovals: List<CreditShareApproval> = emptyList(),
)
