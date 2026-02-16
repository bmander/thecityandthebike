package com.thecityandthebike.data.model

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {

    @Test
    fun `successful response with body returns Success`() = runTest {
        val result = safeApiCall { Response.success("hello") }

        assertTrue(result is ApiResult.Success)
        assertEquals("hello", (result as ApiResult.Success).data)
    }

    @Test
    fun `successful response with null body returns Server error`() = runTest {
        val result = safeApiCall<String> { Response.success(null) }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals("Empty response", (error as AppError.Server).message)
    }

    @Test
    fun `401 response returns Auth error`() = runTest {
        val result = safeApiCall<String> {
            Response.error(401, "Unauthorized".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals(401, (error as AppError.Auth).code)
    }

    @Test
    fun `403 response returns Auth error`() = runTest {
        val result = safeApiCall<String> {
            Response.error(403, "Forbidden".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals(403, (error as AppError.Auth).code)
    }

    @Test
    fun `422 response returns Validation error`() = runTest {
        val result = safeApiCall<String> {
            Response.error(422, "Invalid field".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Validation)
    }

    @Test
    fun `500 response returns Server error`() = runTest {
        val result = safeApiCall<String> {
            Response.error(500, "Internal Server Error".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
    }

    @Test
    fun `503 response returns Server error`() = runTest {
        val result = safeApiCall<String> {
            Response.error(503, "Service Unavailable".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(503, (error as AppError.Server).code)
    }

    @Test
    fun `IOException returns Network error`() = runTest {
        val result = safeApiCall<String> { throw IOException("timeout") }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Network)
        assertEquals("timeout", (error as AppError.Network).cause.message)
    }

    @Test
    fun `generic Exception returns Unknown error`() = runTest {
        val result = safeApiCall<String> { throw RuntimeException("unexpected") }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Unknown)
        assertEquals("unexpected", (error as AppError.Unknown).cause.message)
    }

    @Test
    fun `409 with JSON body extracts detail field`() = runTest {
        val jsonBody = """{"detail":"You've already captured this today!"}"""
        val result = safeApiCall<String> {
            Response.error(409, jsonBody.toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(409, (error as AppError.Server).code)
        assertEquals("You've already captured this today!", error.message)
    }

    @Test
    fun `non-JSON error body uses raw string as fallback`() = runTest {
        val result = safeApiCall<String> {
            Response.error(400, "plain text error".toResponseBody())
        }

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals("plain text error", (error as AppError.Server).message)
    }
}
