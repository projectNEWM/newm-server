package io.newm.server.aws.s3.model

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConditionTests {
    @Test
    fun testEncodeToString() = runBlocking {
        assertEquals(
            """["starts-with","key","value"]""",
            Json.encodeToString(StartsWithCondition("key", "value"))
        )
        assertEquals(
            """["content-length-range",12345,678910]""",
            Json.encodeToString(ContentLengthRangeCondition(12345, 678910))
        )
        assertEquals(
            """["eq","acl","public-read"]""",
            Json.encodeToString(EqualCondition("acl", "public-read"))
        )
        assertEquals(
            """{"acl":"public-read"}""",
            Json.encodeToString(MapCondition("acl", "public-read"))
        )
    }

    @Test
    fun testPolymorphicEncoding() = runBlocking {
        val conditions = listOf(
            StartsWithCondition("key", "value"),
            ContentLengthRangeCondition(12345, 678910),
            EqualCondition("acl", "public-read"),
            MapCondition("acl", "public-read")
        )

        val conditionsString = Json.encodeToString(conditions)
        assertEquals(
            """[["starts-with","key","value"],["content-length-range",12345,678910],["eq","acl","public-read"],{"acl":"public-read"}]""",
            conditionsString
        )
    }
}
