package com.helloclue.sse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SseTest {

    @Test
    fun `is valid response should return true when the response code is 200`() {
        assertTrue { Sse.isValidResponseCode(200) }
    }

    @Test
    fun `is recoverable response code should true when response code is 50X except 501`() {
        for (code in 500..504) {
            val expected = code != 501
            assertEquals(expected, Sse.isRecoverableResponseCode(code))
        }
    }

    @Test
    fun `is no content response should return true when the response code is 204`() {
        assertTrue { Sse.isNoContentResponseCode(204) }
    }

    @Test
    fun `is valid content type true when media type is text and sub type is event-stream`() {
        assertTrue { Sse.isValidContentType(mediaType = "text", mediaSubType = "event-stream") }
    }
}