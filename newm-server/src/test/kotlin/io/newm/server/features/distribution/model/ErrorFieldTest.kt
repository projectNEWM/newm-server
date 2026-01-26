package io.newm.server.features.distribution.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ErrorFieldTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
        }

    @Test
    fun `test parsing ErrorField with simple string message`() {
        val jsonString =
            """
            {
                "fields": "Album Status",
                "message": "Album Status is draft"
            }
            """.trimIndent()

        val result: ErrorField = json.decodeFromString(jsonString)

        assertThat(result.fields).isEqualTo("Album Status")
        assertThat(result.message).isInstanceOf(ErrorFieldMessage.SimpleMessage::class.java)
        val simpleMessage = result.message as ErrorFieldMessage.SimpleMessage
        assertThat(simpleMessage.value).isEqualTo("Album Status is draft")
    }

    @Test
    fun `test parsing ErrorField with complex nested message object`() {
        val jsonString =
            """
            {
                "message": {
                    "message": [
                        "This album is already distributed to some outlets with release date 20/01/2026. Please choose the same date or a date before this."
                    ],
                    "success": false,
                    "errorFields": "date_original_release"
                }
            }
            """.trimIndent()

        val result: ErrorField = json.decodeFromString(jsonString)

        assertThat(result.fields).isNull()
        assertThat(result.message).isInstanceOf(ErrorFieldMessage.ComplexMessage::class.java)
        val complexMessage = result.message as ErrorFieldMessage.ComplexMessage
        assertThat(complexMessage.messages).hasSize(1)
        assertThat(complexMessage.messages?.first()).contains("This album is already distributed")
        assertThat(complexMessage.success).isFalse()
        assertThat(complexMessage.errorFields).isEqualTo("date_original_release")
    }

    @Test
    fun `test parsing ValidateData with mixed error_fields`() {
        val jsonString =
            """
            {
                "album_status": {
                    "status_code": 1021,
                    "status_name": "Draft"
                },
                "error_fields": [
                    {
                        "message": {
                            "message": [
                                "This album is already distributed to some outlets with release date 20/01/2026. Please choose the same date or a date before this."
                            ],
                            "success": false,
                            "errorFields": "date_original_release"
                        }
                    },
                    {
                        "fields": "Album Status",
                        "message": "Album Status is draft"
                    }
                ]
            }
            """.trimIndent()

        val result: ValidateData = json.decodeFromString(jsonString)

        assertThat(result.albumStatus.statusCode).isEqualTo(1021)
        assertThat(result.albumStatus.statusName).isEqualTo("Draft")
        assertThat(result.errorFields).hasSize(2)

        // First error field: complex message
        val firstError = result.errorFields!![0]
        assertThat(firstError.fields).isNull()
        assertThat(firstError.message).isInstanceOf(ErrorFieldMessage.ComplexMessage::class.java)
        val complexMessage = firstError.message as ErrorFieldMessage.ComplexMessage
        assertThat(complexMessage.messages).hasSize(1)
        assertThat(complexMessage.errorFields).isEqualTo("date_original_release")

        // Second error field: simple message
        val secondError = result.errorFields!![1]
        assertThat(secondError.fields).isEqualTo("Album Status")
        assertThat(secondError.message).isInstanceOf(ErrorFieldMessage.SimpleMessage::class.java)
        val simpleMessage = secondError.message as ErrorFieldMessage.SimpleMessage
        assertThat(simpleMessage.value).isEqualTo("Album Status is draft")
    }

    @Test
    fun `test parsing full ValidateAlbumResponse with error_fields`() {
        val jsonString =
            """
            {
                "message": "You can't distribute this album now. Please enter all required details for this album",
                "success": true,
                "data": {
                    "error_fields": [
                        {
                            "message": {
                                "message": [
                                    "This album is already distributed to some outlets with release date 20/01/2026. Please choose the same date or a date before this."
                                ],
                                "success": false,
                                "errorFields": "date_original_release"
                            }
                        },
                        {
                            "fields": "Album Status",
                            "message": "Album Status is draft"
                        }
                    ],
                    "album_status": {
                        "status_code": 1021,
                        "status_name": "Draft"
                    }
                }
            }
            """.trimIndent()

        val result: ValidateAlbumResponse = json.decodeFromString(jsonString)

        assertThat(result.message).isEqualTo("You can't distribute this album now. Please enter all required details for this album")
        assertThat(result.success).isTrue()
        assertThat(result.validateData.albumStatus.statusCode).isEqualTo(1021)
        assertThat(result.validateData.errorFields).hasSize(2)

        // Verify complex message
        val complexError = result.validateData.errorFields!![0]
        val complexMessage = complexError.message as ErrorFieldMessage.ComplexMessage
        assertThat(complexMessage.messages?.first()).contains("already distributed")

        // Verify simple message
        val simpleError = result.validateData.errorFields!![1]
        val simpleMessage = simpleError.message as ErrorFieldMessage.SimpleMessage
        assertThat(simpleMessage.value).isEqualTo("Album Status is draft")
    }

    @Test
    fun `test parsing ErrorField with null message`() {
        val jsonString =
            """
            {
                "fields": "Some Field"
            }
            """.trimIndent()

        val result: ErrorField = json.decodeFromString(jsonString)

        assertThat(result.fields).isEqualTo("Some Field")
        assertThat(result.message).isNull()
    }

    @Test
    fun `test parsing complex message with multiple messages in array`() {
        val jsonString =
            """
            {
                "message": {
                    "message": [
                        "First error message",
                        "Second error message",
                        "Third error message"
                    ],
                    "success": false,
                    "errorFields": "some_field"
                }
            }
            """.trimIndent()

        val result: ErrorField = json.decodeFromString(jsonString)

        val complexMessage = result.message as ErrorFieldMessage.ComplexMessage
        assertThat(complexMessage.messages).hasSize(3)
        assertThat(complexMessage.messages).containsExactly(
            "First error message",
            "Second error message",
            "Third error message"
        )
    }
}
