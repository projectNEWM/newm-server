package io.newm.server.features.collaboration.model

enum class CollaborationStatus {
    Editing, // Editing BEFORE artist accepts the stream token agreement
    Waiting, // Waiting collaborator response AFTER artist accepts the stream token agreement
    Accepted, // Accepted by collaborator
    Rejected, // Rejected by collaborator
}
