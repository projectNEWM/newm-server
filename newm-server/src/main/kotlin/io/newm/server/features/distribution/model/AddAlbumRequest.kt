package io.newm.server.features.distribution.model

import io.newm.shared.serialization.DMYLocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class AddAlbumRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("name")
    val name: String?,
    @SerialName("artists")
    val artistIds: List<Long>?,
    @SerialName("subscription_id")
    val subscriptionId: Long,
    @SerialName("ean_upc")
    val eanUpc: String?,
    @SerialName("label_id")
    val labelId: Long?,
    @SerialName("product_type")
    val productType: String, // should always be set to "single"
    @SerialName("original_release_date")
    @Serializable(with = DMYLocalDateSerializer::class)
    val originalReleaseDate: LocalDate?,
    @SerialName("product_code_type")
    val productCodeType: String, // can be "ean" or "upc"
    @SerialName("code_auto_generate")
    val codeAutoGenerate: Boolean,
    @SerialName("product_format")
    val productFormat: String, // should always be "stereo"
    @SerialName("cover_image")
    val coverImage: CoverImage,
    @SerialName("tracks")
    val tracks: List<Track>
)
