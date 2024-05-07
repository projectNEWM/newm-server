package io.newm.server.features.staticmembers

class StaticMembers {
    companion object {
        private const val AUTH_PATH = "/v1/cloudinary/sign"

        fun getAuthPath(): String {
            return AUTH_PATH
        }

        private const val TIMESTAMP = "timestamp"

        fun getTimestamp(): String {
            return TIMESTAMP
        }

        private const val CLOUDINARY_URL = "cloudinary.url"

        fun getCloudinaryUrl(): String {
            return CLOUDINARY_URL
        }
    }
}
