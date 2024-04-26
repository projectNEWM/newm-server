package io.newm.server.features.song.model

enum class ReleaseBarcodeType {
    Upc, // Universal Product Codes (UPC) - 0
    Ean, // International Article Number (EAN) - 1
    Jan, // Japanese Article Number (JAN) - 2
}

fun String.toSongBarcodeType(): ReleaseBarcodeType {
    return if (this.equals("upc", ignoreCase = true)) {
        ReleaseBarcodeType.Upc
    } else if (this.equals("ean", ignoreCase = true)) {
        ReleaseBarcodeType.Ean
    } else if (this.equals("jan", ignoreCase = true)) {
        ReleaseBarcodeType.Jan
    } else {
        throw IllegalArgumentException("Invalid barcode type: $this")
    }
}
