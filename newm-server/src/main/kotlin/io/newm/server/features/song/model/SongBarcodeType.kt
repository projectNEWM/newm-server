package io.newm.server.features.song.model

enum class SongBarcodeType {
    Upc, // Universal Product Codes (UPC) - 0
    Ean, // International Article Number (EAN) - 1
    Jan, // Japanese Article Number (JAN) - 2
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
