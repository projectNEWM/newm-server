package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountBalance(
    val controlledWinc: String,
    val winc: String,
    val effectiveBalance: String,
    val receivedApprovals: List<CreditShareApproval> = emptyList(),
    val givenApprovals: List<CreditShareApproval> = emptyList(),
)
