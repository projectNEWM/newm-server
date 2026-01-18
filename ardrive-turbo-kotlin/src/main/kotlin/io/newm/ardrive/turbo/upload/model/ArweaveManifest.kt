package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class ArweaveManifest(
    val manifest: String = "arweave/paths",
    val version: String = "0.2.0",
    val index: ManifestIndex,
    val paths: Map<String, ManifestPath>,
    val fallback: ManifestFallback? = null,
)

@Serializable
data class ManifestIndex(
    val path: String,
)

@Serializable
data class ManifestPath(
    val id: String,
)

@Serializable
data class ManifestFallback(
    val id: String,
)
