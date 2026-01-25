package io.newm.server.compression

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.condition
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.compression.zstd.zstd
import io.ktor.server.request.path
import io.newm.shared.ktx.getConfigLong
import io.newm.shared.ktx.getConfigStrings

fun Application.installCompression() {
    val minSize = environment.getConfigLong("compression.minSize")
    val pathPrefixes = environment.getConfigStrings("compression.pathPrefixes")

    install(Compression) {
        minimumSize(minSize)
        condition {
            val path = request.path()
            pathPrefixes.any(path::startsWith)
        }

        zstd { priority = 1.0 }
        gzip { priority = 0.9 }
        deflate { priority = 0.8 }
    }
}
