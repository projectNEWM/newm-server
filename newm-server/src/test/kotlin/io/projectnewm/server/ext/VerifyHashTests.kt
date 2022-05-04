package io.projectnewm.server.ext

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

// https://passwordsgenerator.net/
private val passwords = listOf(
    "K9Z<cC&zg9\\zEq.}",
    "+efAAR$;y`v24yWg",
    "p`!Mj)M9u\"G:b9\".",
    "zBKApxzY_4y\\*8-<",
    "m32!>_'N)~ZLu2H!",
    "\\Ake#:Bv,b4={k>G",
    "2f*/,!eX6CnG4eY5",
    "['~m6nT?UM^D^/~*",
    "~=8({Gp4\\~!<w@r",
    "D:yKfCh9>6X>XCYZ"
)

class VerifyHashTest {

    @Test
    fun testVerifyValid() {
        for (password in passwords) {
            val hash = password.toHash()
            Truth.assertWithMessage(password).that(password.verify(hash)).isTrue()
        }
    }

    @Test
    fun testVerifyInvalid() {
        for (password in passwords) {
            val hash = password.toHash()
            val password1 = '0' + password.substring(1)
            val password2 = password.substring(0, password.length - 1) + "0"
            Truth.assertWithMessage(password).that(password1.verify(hash)).isFalse()
            Truth.assertWithMessage(password).that(password2.verify(hash)).isFalse()
        }
    }
}
