package io.newm.shared.ktx

import com.google.common.truth.Truth.assertWithMessage
import org.junit.jupiter.api.Test

// Test data adapted from https://mathiasbynens.be/demo/url-regex
private val validUrls = listOf(
    "http://foo.com/blah_blah",
    "http://foo.com/blah_blah/",
    "http://foo.com/blah_blah_(wikipedia)",
    "http://foo.com/blah_blah_(wikipedia)_(again)",
    "http://www.example.com/wpstyle/?p=364",
    "https://www.example.com/foo/?bar=baz&inga=42&quux",
    "http://✪df.ws/123",
    "http://userid:password@example.com:8080",
    "http://userid:password@example.com:8080/",
    "http://userid@example.com",
    "http://userid@example.com/",
    "http://userid@example.com:8080",
    "http://userid@example.com:8080/",
    "http://userid:password@example.com",
    "http://userid:password@example.com/",
    "http://142.42.1.1/",
    "http://142.42.1.1:8080/",
    "http://➡.ws/䨹",
    "http://⌘.ws",
    "http://⌘.ws/",
    "http://foo.com/blah_(wikipedia)#cite-1",
    "http://foo.com/blah_(wikipedia)_blah#cite-1",
    "http://foo.com/unicode_(✪)_in_parens",
    "http://foo.com/(something)?after=parens",
    "http://☺.damowmow.com/",
    "http://code.google.com/events/#&product=browser",
    "http://j.mp",
    "ftp://foo.bar/baz",
    "http://foo.bar/?q=Test%20URL-encoded%20stuff",
    "http://مثال.إختبار",
    "http://例子.测试",
    "http://उदाहरण.परीक्षा",
    "http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com",
    "http://1337.net",
    "http://a.b-c.de",
    "http://223.255.255.254)",
    "https://lh3.googleusercontent.com/a-/AOh14Gis1zQ1NZ0wrqj8dhscve8gE2-T2iM_xkCItzCQ34jw5KH0kyMReOQJ558j74r-80Jsn3unLCYsUQoC-LqtTrq03qvtK0dM2aOR0c-zphcQoDtA3DC80CVgPF5wzIKz6BJ6Tm47L-hziAYuuyEz4F-_YAZdUXFtspkph_qtm1S8i-1zS4A_D9K1e0oYOxHwgIbMGBVDXDdYNQYk6-9GiOCE1O8g7RYAulMoZEoEhLRBHGAXdg39_b9M4TvFN9gO4f04qJyLbKCtj1ROvv5_Fo7dcmk1-xmIgOglOBmjj214G0euJiJVn-Fa-VN5-IcOdrJzesfQhLInQlDnD_LiyosHjANoNbKfKAzVpxSGqTn75GTfAfppqtbQ_KJlrQlur9AHOBSz6eF8DQzUJqMrZvRJh1GozfO2P0I-8fkpZDjVxEHsm7LRjbuzFsytQSIfF4vWcAhViHj1ryvUtR0oZdsxatGR-V_EtVF9Jd_uxKcGLuvEa4Hn65OC5EWuP08r2w6uYu0OZTLtpYQptDyiuJLgMsY1KWLbsNN3QxT1sLyaM9Om5WlxJSdd6SV6YvOxPrsKLOpZ-uspxJB8z3PLcfeNTHUcGMRlD8o6_lSovVhl-FzadvSDvbseoSgk0w3WZSTrqzXZ3dvKwUehOJbDU85TQlSPbr4A27pLEV9BDD3BhiM_jNhljWENUrmuOe_idIMrGMzI9XQcngwIVUv0ULaQiu-LXuikOsLsw-vCf9NcYNeTyIoCE4TJa8IThvgz5YKL4w=s96-c",
    "s3://test",
    "ar://1234"
)

private val invalidUrls = listOf(
    "http://#",
    "http://##",
    "http://##/",
    "http://foo.bar?q=Spaces should be encoded",
    "//",
    "//a",
    "///a",
    "///",
    "foo . com",
    "http:// shouldfail.com",
    ":// should fail",
    "http://foo.bar/foo(bar)baz quux",
)

class IsValidUrlTests {

    @Test
    fun testIsValidUrlWithValidUrls() {
        for (url in validUrls) {
            assertWithMessage(url).that(url.isValidUrl()).isTrue()
        }
    }

    @Test
    fun testIsValidUrlWithInvalidUrls() {
        for (url in invalidUrls) {
            assertWithMessage(url).that(url.isValidUrl()).isFalse()
        }
    }
}
