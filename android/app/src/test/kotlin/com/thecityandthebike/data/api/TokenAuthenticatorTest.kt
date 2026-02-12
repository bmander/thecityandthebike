package com.thecityandthebike.data.api

import com.thecityandthebike.data.local.TokenManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var refreshClient: OkHttpClient
    private lateinit var authenticator: TokenAuthenticator
    private lateinit var mockCall: okhttp3.Call

    @Before
    fun setup() {
        tokenManager = mockk(relaxed = true)
        refreshClient = mockk()
        mockCall = mockk<okhttp3.Call>()
        every { refreshClient.newCall(any()) } returns mockCall
        authenticator = TokenAuthenticator(tokenManager, refreshClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun make401Response(
        token: String = "old_token",
        priorResponse: Response? = null
    ): Response {
        val request = Request.Builder()
            .url("https://example.com/api/data")
            .header("Authorization", "Bearer $token")
            .build()
        val response = mockk<Response>()
        every { response.request } returns request
        every { response.priorResponse } returns priorResponse
        return response
    }

    private fun stubRefreshSuccess(
        accessToken: String = "new_access",
        refreshToken: String = "new_refresh"
    ) {
        val body = """{"access_token":"$accessToken","refresh_token":"$refreshToken"}"""
        val refreshResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com/auth/refresh").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns refreshResponse
    }

    private fun stubRefreshFailure(code: Int = 401) {
        val refreshResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com/auth/refresh").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Unauthorized")
            .body("{}".toResponseBody())
            .build()
        every { mockCall.execute() } returns refreshResponse
    }

    @Test
    fun `successful token refresh re-authenticates the request`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "old_refresh"
        stubRefreshSuccess("new_access", "new_refresh")

        val result = authenticator.authenticate(null, make401Response())

        assertNotNull(result)
        assertEquals("Bearer new_access", result!!.header("Authorization"))
        verify { tokenManager.saveTokens("new_access", "new_refresh") }
    }

    @Test
    fun `gives up after 2 attempts`() {
        // Build a response chain: prior(401) -> current(401) => count = 2
        val priorResponse = mockk<Response>()
        every { priorResponse.priorResponse } returns null
        val response = make401Response(priorResponse = priorResponse)

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `clears tokens when refresh token is null`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns null

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
        verify(exactly = 0) { refreshClient.newCall(any()) }
    }

    @Test
    fun `clears tokens when refresh request throws network error`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "some_refresh"
        every { mockCall.execute() } throws java.io.IOException("Network error")

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `clears tokens when refresh response is unsuccessful`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "some_refresh"
        stubRefreshFailure(401)

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `clears tokens when refresh response body is invalid JSON`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "some_refresh"
        val refreshResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com/auth/refresh").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("not valid json".toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns refreshResponse

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `uses already-refreshed token from another thread`() {
        // The request was sent with "old_token", but another thread already refreshed to "new_token"
        every { tokenManager.getToken() } returns "new_token"

        val result = authenticator.authenticate(null, make401Response(token = "old_token"))

        assertNotNull(result)
        assertEquals("Bearer new_token", result!!.header("Authorization"))
        // Should not have attempted a refresh call
        verify(exactly = 0) { refreshClient.newCall(any()) }
    }

    @Test
    fun `responseCount correctly counts prior responses`() {
        // Chain: prior2 -> prior1 -> current => count = 3, should give up (>= 2)
        val prior2 = mockk<Response>()
        every { prior2.priorResponse } returns null

        val prior1 = mockk<Response>()
        every { prior1.priorResponse } returns prior2

        val response = make401Response(priorResponse = prior1)

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `clears tokens when refresh response missing access_token field`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "some_refresh"
        val body = """{"refresh_token":"new_refresh"}"""
        val refreshResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com/auth/refresh").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns refreshResponse

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `clears tokens when refresh response missing refresh_token field`() {
        every { tokenManager.getToken() } returns "old_token"
        every { tokenManager.getRefreshToken() } returns "some_refresh"
        val body = """{"access_token":"new_access"}"""
        val refreshResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com/auth/refresh").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        every { mockCall.execute() } returns refreshResponse

        val result = authenticator.authenticate(null, make401Response())

        assertNull(result)
        verify { tokenManager.clearToken() }
    }
}
