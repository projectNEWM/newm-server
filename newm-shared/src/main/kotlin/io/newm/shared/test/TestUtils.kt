package io.newm.shared.test

object TestUtils {
    fun isRunningInTest(): Boolean =
        try {
            Class.forName("org.junit.jupiter.api.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
}
