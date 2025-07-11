package io.newm.server.features.ethereum.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EthereumNft(
    @SerialName("contract")
    val contract: Contract,
    @SerialName("tokenId")
    val tokenId: String,
    @SerialName("tokenType")
    val tokenType: String,
    @SerialName("name")
    val name: String?,
    @SerialName("image")
    val image: Media?,
    @SerialName("animation")
    val animation: Media?,
    @SerialName("raw")
    val raw: Raw?,
    @SerialName("balance")
    val balance: Long
) {
    @Serializable
    data class Contract(
        @SerialName("address")
        val address: String,
        @SerialName("name")
        val name: String?
    )

    @Serializable
    data class Media(
        @SerialName("contentType")
        val contentType: String?,
        @SerialName("cachedUrl")
        val cachedUrl: String?,
        @SerialName("originalUrl")
        val originalUrl: String?
    )

    @Serializable
    data class Raw(
        @SerialName("metadata")
        val metadata: Metadata?
    )

    @Serializable
    data class Metadata(
        @SerialName("name")
        val name: String?,
        @SerialName("image")
        val image: String?,
        @SerialName("animation_url")
        val animationUrl: String?,
        @SerialName("attributes")
        val attributes: List<Attribute>?
    )

    @Serializable
    data class Attribute(
        @SerialName("trait_type")
        val traitType: String,
        @SerialName("value")
        val value: String
    )
}
