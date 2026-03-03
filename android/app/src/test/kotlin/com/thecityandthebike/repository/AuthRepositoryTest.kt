package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.TokenResponse
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.UserResponse
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
    fun `register 422 validation error should parse first message from detail list`() = runTest {
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
        assertEquals("invalid email", error.displayMessage)
    }

    @Test
    fun `register error with string detail should parse message`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(
            400,
            """{"detail": "Username is not available"}""".toResponseBody()
        )

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(400, (error as AppError.Server).code)
        assertEquals("Username is not available", error.displayMessage)
    }

    @Test
    fun `register with unparseable error body should fall back gracefully`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass"))
        } returns Response.error(
            500,
            "not json".toResponseBody()
        )

        val result = repository.register("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
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

    // --- android_id support ---

    @Test
    fun `login passes androidId to LoginRequest`() = runTest {
        val tokenResponse = TokenResponse(accessToken = "tok", refreshToken = "ref", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("user", "pass", "device-123")) } returns Response.success(tokenResponse)
        coEvery { apiService.getCurrentUser() } returns Response.success(
            UserResponse(userId = "u1", username = "user")
        )

        val result = repository.login("user", "pass", androidId = "device-123")

        assertTrue(result is ApiResult.Success)
        coVerify { apiService.login(LoginRequest("user", "pass", "device-123")) }
    }

    @Test
    fun `register passes androidId to RegisterRequest`() = runTest {
        val messageResponse = MessageResponse(msg = "User created")
        coEvery {
            apiService.register(RegisterRequest("user", "pass", "device-456"))
        } returns Response.success(messageResponse)

        val result = repository.register("user", "pass", androidId = "device-456")

        assertTrue(result is ApiResult.Success)
        coVerify { apiService.register(RegisterRequest("user", "pass", "device-456")) }
    }

    // --- login fetches isAdmin ---

    @Test
    fun `login success fetches current user and saves isAdmin true`() = runTest {
        val tokenResponse = TokenResponse(accessToken = "tok", refreshToken = "ref", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("admin", "pass")) } returns Response.success(tokenResponse)
        coEvery { apiService.getCurrentUser() } returns Response.success(
            UserResponse(userId = "u1", username = "admin", isAdmin = true)
        )

        repository.login("admin", "pass")

        verify { tokenManager.saveIsAdmin(true) }
    }

    @Test
    fun `login success fetches current user and saves isAdmin false`() = runTest {
        val tokenResponse = TokenResponse(accessToken = "tok", refreshToken = "ref", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.success(tokenResponse)
        coEvery { apiService.getCurrentUser() } returns Response.success(
            UserResponse(userId = "u1", username = "user", isAdmin = false)
        )

        repository.login("user", "pass")

        verify { tokenManager.saveIsAdmin(false) }
    }

    @Test
    fun `login success still succeeds if getCurrentUser fails`() = runTest {
        val tokenResponse = TokenResponse(accessToken = "tok", refreshToken = "ref", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.success(tokenResponse)
        coEvery { apiService.getCurrentUser() } throws java.io.IOException("network error")

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Success)
        verify { tokenManager.saveTokens("tok", "ref") }
    }

    // --- login 403 banned handling ---

    @Test
    fun `login 403 with banned message returns auth error`() = runTest {
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.error(
            403,
            """{"detail": {"msg": "This account has been banned"}}""".toResponseBody()
        )

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals(403, (error as AppError.Auth).code)
        assertEquals("This account has been banned", error.message)
    }

    @Test
    fun `login 403 with device banned message returns auth error`() = runTest {
        coEvery { apiService.login(LoginRequest("user", "pass", "banned-device")) } returns Response.error(
            403,
            """{"detail": {"msg": "This device has been banned"}}""".toResponseBody()
        )

        val result = repository.login("user", "pass", androidId = "banned-device")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals("This device has been banned", (error as AppError.Auth).message)
    }

    @Test
    fun `login 403 with unparseable body falls back to default message`() = runTest {
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.error(
            403,
            "not json".toResponseBody()
        )

        val result = repository.login("user", "pass")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals("This account or device has been banned", (error as AppError.Auth).message)
    }

    // --- register 403 banned device ---

    @Test
    fun `register 403 with banned device returns server error with message`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "pass", "banned-device"))
        } returns Response.error(
            403,
            """{"detail": {"msg": "This device has been banned"}}""".toResponseBody()
        )

        val result = repository.register("user", "pass", androidId = "banned-device")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(403, (error as AppError.Server).code)
        assertEquals("This device has been banned", error.displayMessage)
    }
}
