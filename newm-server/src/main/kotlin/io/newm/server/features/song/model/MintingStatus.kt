package io.newm.server.features.song.model

enum class MintingStatus {
    Undistributed, // Song has been uploaded/created. User may still be editing metadata for it. - 0
    StreamTokenAgreementApproved, // Artist has approved the Agreement /w placeholder for ISRC number - 1
    MintingPaymentRequested, // We've created a payment address and presented that to the Artist's wallet for payment. - 2
    MintingPaymentSubmitted, // Payment was submitted to the blockchain. - 3
    MintingPaymentReceived, // Payment has been received in the payment address and 3 blocks have occurred since. - 4
    AwaitingAudioEncoding, // We're waiting for audio encoding to be completed - 5
    AwaitingCollaboratorApproval, // We're waiting for collaborators to approve their percentages - 6
    ReadyToDistribute, // Ready for the distribution team to send all the song information to distribution partners. - 7
    SubmittedForDistribution, // Distribution sets this status once they have uploaded song/metadata for distribution. - 8
    Distributed, // Distribution partner has successfully deployed to streaming platforms. ISRC number added to song record. - 9
    Declined, // Distribution partner rejected song for some reason. Manual triage required. - 10
    Pending, // Song has entered the queue for minting - 11
    Minted, // Song has completed the minting process and stream tokens are in the Artist wallet. - 12

    // Error statuses
    MintingPaymentTimeout, // We timed out waiting for payment. - 13
    MintingPaymentException, // An exception occurred while waiting for payment. - 14
    DistributionException, // An exception occurred while distributing. - 15
    SubmittedForDistributionException, // An exception occurred while checking submitted for distribution. - 16
    ArweaveUploadException, // An exception occurred while uploading to Arweave. - 17
    MintingException, // An exception occurred while minting. - 18
    /**
     * --- IMPORTANT! ---
     * All new statuses should be added to the end of this list. We don't want to change the ordinal of any existing
     * statuses that exist in our database.
     */
}
