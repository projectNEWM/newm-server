package io.newm.server.features.song.model

enum class SongBarcodeType {
    Upc, // Universal Product Codes (UPC)
    Ean, // International Article Number (EAN)
    Jan, // Japanese Article Number (JAN)
}

fun String.toSongBarcodeType(): SongBarcodeType {
    return if (this.equals("upc", ignoreCase = true)) {
        SongBarcodeType.Upc
    } else if (this.equals("ean", ignoreCase = true)) {
        SongBarcodeType.Ean
    } else if (this.equals("jan", ignoreCase = true)) {
        SongBarcodeType.Jan
    } else {
        throw IllegalArgumentException("Invalid barcode type: $this")
    }
}
