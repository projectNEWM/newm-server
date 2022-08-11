package io.projectnewm.server.ext

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

private val validPasswords = listOf(
    "Password1",
    "Welcome Home Sold1er",
    "nAkY2M36",
    "Ck8Y82HA",
    "97TeFQ5!",
    "E9J\$U8*Rn4Zz66geb",
    "PeWsuHh*zu2r4&qv1",
    "y88yb3QgO0BIieF!L88ttaqR\$b0H*Tu",
    "DXcoT1R!1xi69*v7F"
)

private val invalidPasswords = listOf(
    "",
    "passwordAddress",
    "#@%^%#$@#$@#",
    "あいうえお",
    "\"(),:;<>[\\]",
    "password",
    "dUWFYfD^%@GK&T*t\$kc@PNgQVaIvEtZ",
    "b\$v541ihcp1493w^n#ox^8p*u",
    "@#^#@&\$!%\$%#&^!&*#&&^!^*\$"
)

class IsValidPasswordTests {
    @Test
    fun testIsValidPasswordWithValidPasswords() {
        for (password in validPasswords) {
            Truth.assertWithMessage(password).that(password.isValidPassword()).isTrue()
        }
    }

    @Test
    fun testIsValidPasswordWithInvalidPasswords() {
        for (password in invalidPasswords) {
            Truth.assertWithMessage(password).that(password.isValidPassword()).isFalse()
        }
    }
}