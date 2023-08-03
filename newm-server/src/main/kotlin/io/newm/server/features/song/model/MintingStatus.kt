package io.newm.server.features.song.model

enum class MintingStatus {
    Undistributed, // Song has been uploaded/created. User may still be editing metadata for it.
    StreamTokenAgreementApproved, // Artist has approved the Agreement /w placeholder for ISRC number

    // TODO: Add statuses to ensure the song successfully encodes for streaming.

    MintingPaymentRequested, // We've created a payment address and presented that to the Artist's wallet for payment.
    MintingPaymentReceived, // Payment has been received in the payment address and 3 blocks have occurred since.
    AwaitingCollaboratorApproval, // We're waiting for collaborators to approve their percentages
    ReadyToDistribute, // Ready for the distribution team to send all the song information to distribution partners.
    SubmittedForDistribution, // Distribution sets this status once they have uploaded song/metadata for distribution.
    Distributed, // Distribution partner has successfully deployed to streaming platforms. ISRC number added to song record.
    Declined, // Distribution partner rejected song for some reason. Manual triage required.
    Pending, // Song has entered the queue for minting
    Minted, // Song has completed the minting process and stream tokens are in the Artist wallet.
}
