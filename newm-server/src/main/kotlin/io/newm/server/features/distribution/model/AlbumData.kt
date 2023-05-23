package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumData(
    @SerialName("cover_image")
    val coverImage: String,
    @SerialName("subscription")
    val subscription: SubscriptionX,
    @SerialName("ean_upc")
    val eanUpc: String,
    @SerialName("is_active")
    val isActive: Int,
    @SerialName("product_format")
    val productFormat: String,
    @SerialName("original_release_date")
    val originalReleaseDate: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("release_id")
    val releaseId: Long,
    @SerialName("pre_save_link")
    val preSaveLink: String,
    @SerialName("tracks")
    val tracks: List<TrackX>,
    @SerialName("product_type")
    val productType: String,
    @SerialName("album_status")
    val albumStatus: AlbumStatus,
    @SerialName("artist")
    val artist: List<Artist>,
    @SerialName("spatial_product_code_type")
    val spatialProductCodeType: String,
    @SerialName("disapprove_message")
    val disapproveMessage: String,
    @SerialName("product_code_type")
    val productCodeType: String,
    @SerialName("outlets")
    val outlets: List<Outlet>,
    @SerialName("removable")
    val removable: String,
    @SerialName("track_count")
    val trackCount: Int
)
