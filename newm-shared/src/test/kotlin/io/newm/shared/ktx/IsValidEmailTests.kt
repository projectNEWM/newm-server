package io.newm.shared.ktx

import com.google.common.truth.Truth.assertWithMessage
import org.junit.jupiter.api.Test

// Test data adapted from https://codefool.tumblr.com/post/15288874550/list-of-valid-and-invalid-email-addresses
private val validEmails =
    listOf(
        "email@example.com",
        "firstname.lastname@example.com",
        "email@subdomain.example.com",
        "firstname+lastname@example.com",
        "email@123.123.123.123",
        "1234567890@example.com",
        "email@example-one.com",
        "_______@example.com",
        "email@example.name",
        "email@example.museum",
        "email@example.co.jp",
        "firstname-lastname@example.com"
    )

private val invalidEmails =
    listOf(
        "plainaddress",
        "#@%^%#$@#$@#.com",
        "@example.com",
        "Joe Smith <email@example.com>",
        "email.example.com",
        "email@example@example.com",
        "あいうえお@example.com",
        "email@example.com (Joe Smith)",
        "email@example",
        "email@-example.com",
        "email@example..com",
        "\"(),:;<>[\\]@example.com",
        "just\"not\"right@example.com",
        "this\\ is\"really\"not\\\\allowed@example.com"
    )

class IsValidEmailTests {
    @Test
    fun testIsValidEmailWithValidEmails() {
        for (email in validEmails) {
            assertWithMessage(email).that(email.isValidEmail()).isTrue()
        }
    }

    @Test
    fun testIsValidEmailWithInvalidEmails() {
        for (email in invalidEmails) {
            assertWithMessage(email).that(email.isValidEmail()).isFalse()
        }
    }
}
