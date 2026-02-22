package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.TokenResponse
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        apiService = mockk()
        tokenManager = mockk(relaxed = true)
        repository = AuthRepository(apiService, tokenManager)
    }

    @Test
    fun `login success should save token and return success`() = runTest {
        val tokenResponse = TokenResponse(accessToken = "test_token", refreshToken = "test_refresh", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.success(tokenResponse)

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Success)
        verify { tokenManager.saveTokens("test_token", "test_refresh") }
    }

    @Test
    fun `login failure should return auth error`() = runTest {
        coEvery { apiService.login(LoginRequest("user", "wrong")) } returns Response.error(
            401,
            "{}".toResponseBody()
        )

        val result = repository.login("user", "wrong")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals("Invalid credentials", (error as AppError.Auth).message)
    }

    @Test
    fun `login network error should return network error`() = runTest {
        coEvery { apiService.login(any()) } throws java.io.IOException("Connection refused")

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `register success should return success`() = runTest {
        val messageResponse = MessageResponse(msg = "User created")
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.success(messageResponse)

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `register conflict should return server error with parsed message`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(
            409,
            """{"detail": {"msg": "User with that username already exists"}}""".toResponseBody()
        )

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(409, (error as AppError.Server).code)
        assertEquals("User with that username already exists", error.displayMessage)
    }

    @Test
    fun `register non-409 error should parse server error body`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(
            400,
            """{"detail": {"msg": "Password too short"}}""".toResponseBody()
        )

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(400, (error as AppError.Server).code)
        assertEquals("Password too short", error.displayMessage)
    }

    @Test
    fun `register with unparseable error body should fall back gracefully`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(
            422,
            """{"detail": [{"loc": ["body", "email"], "msg": "invalid email", "type": "value_error"}]}""".toResponseBody()
        )

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(422, (error as AppError.Server).code)
        assertEquals("Registration failed", error.displayMessage)
    }

    @Test
    fun `register rate limited should return rate limit error`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(429, "{}".toResponseBody())

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.RateLimit)
        assertEquals("Too many attempts. Please try again later.", error.displayMessage)
    }

    @Test
    fun `login rate limited should return rate limit error`() = runTest {
        coEvery {
            apiService.login(LoginRequest("user", "pass"))
        } returns Response.error(429, "{}".toResponseBody())

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.RateLimit)
        assertEquals("Too many attempts. Please try again later.", error.displayMessage)
    }

    @Test
    fun `logout should clear token`() = runTest {
        coEvery { apiService.logout(any()) } returns Response.success(MessageResponse(msg = "Logged out"))
        repository.logout()
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `isLoggedIn should return token manager state`() {
        every { tokenManager.hasToken() } returns true
        assertTrue(repository.isLoggedIn())

        every { tokenManager.hasToken() } returns false
        assertFalse(repository.isLoggedIn())
    }
}
