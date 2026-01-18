package io.newm.ardrive.turbo.upload.util

internal object CreditSharingTags {
    const val SHARE_CREDITS = "x-approve-payment"
    const val SHARED_WINC_AMOUNT = "x-amount"
    const val APPROVAL_EXPIRES_BY_SECONDS = "x-expires-seconds"
    const val REVOKE_CREDITS = "x-delete-payment-approval"
}
