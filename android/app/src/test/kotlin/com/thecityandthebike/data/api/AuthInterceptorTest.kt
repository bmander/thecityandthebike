package com.thecityandthebike.data.api

import com.thecityandthebike.data.local.TokenManager
import io.mockk.*
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var interceptor: AuthInterceptor
    private lateinit var chain: Interceptor.Chain

    @Before
    fun setup() {
        tokenManager = mockk()
        interceptor = AuthInterceptor(tokenManager)
        chain = mockk()
    }

    private fun stubChain(request: Request) {
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }
    }

    @Test
    fun `skips auth header for login endpoint`() {
        val request = Request.Builder().url("https://example.com/auth/login").build()
        stubChain(request)

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertNull(captured.captured.header("Authorization"))
    }

    @Test
    fun `skips auth header for register endpoint`() {
        val request = Request.Builder().url("https://example.com/auth/register").build()
        stubChain(request)

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertNull(captured.captured.header("Authorization"))
    }

    @Test
    fun `skips auth header for refresh endpoint`() {
        val request = Request.Builder().url("https://example.com/auth/refresh").build()
        stubChain(request)

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertNull(captured.captured.header("Authorization"))
    }

    @Test
    fun `skips auth header when token is null`() {
        val request = Request.Builder().url("https://example.com/api/data").build()
        stubChain(request)
        every { tokenManager.getToken() } returns null

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertNull(captured.captured.header("Authorization"))
    }

    @Test
    fun `skips auth header when token is empty`() {
        val request = Request.Builder().url("https://example.com/api/data").build()
        stubChain(request)
        every { tokenManager.getToken() } returns ""

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertNull(captured.captured.header("Authorization"))
    }

    @Test
    fun `adds Authorization Bearer header for non-auth endpoints`() {
        val request = Request.Builder().url("https://example.com/api/data").build()
        stubChain(request)
        every { tokenManager.getToken() } returns "my_token_123"

        interceptor.intercept(chain)

        val captured = slot<Request>()
        verify { chain.proceed(capture(captured)) }
        assertEquals("Bearer my_token_123", captured.captured.header("Authorization"))
    }
}
