package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.TokenResponse
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.repository.AuthRepository
import com.thecityandthebike.data.repository.AuthResult
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
        val tokenResponse = TokenResponse(accessToken = "test_token", tokenType = "bearer")
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.success(tokenResponse)

        val result = repository.login("user", "pass")

        assertTrue(result is AuthResult.Success)
        verify { tokenManager.saveToken("test_token") }
    }

    @Test
    fun `login failure should return error`() = runTest {
        coEvery { apiService.login(LoginRequest("user", "wrong")) } returns Response.error(
            401,
            "{}".toResponseBody()
        )

        val result = repository.login("user", "wrong")

        assertTrue(result is AuthResult.Error)
        assertEquals("Invalid credentials", (result as AuthResult.Error).message)
    }

    @Test
    fun `login network error should return error`() = runTest {
        coEvery { apiService.login(any()) } throws Exception("Network error")

        val result = repository.login("user", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("Network error", (result as AuthResult.Error).message)
    }

    @Test
    fun `register success should return success`() = runTest {
        val messageResponse = MessageResponse(msg = "User created")
        coEvery {
            apiService.register(RegisterRequest("user", "email@test.com", "pass"))
        } returns Response.success(messageResponse)

        val result = repository.register("user", "email@test.com", "pass")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `register conflict should return user exists error`() = runTest {
        coEvery {
            apiService.register(RegisterRequest("user", "email@test.com", "pass"))
        } returns Response.error(409, "{}".toResponseBody())

        val result = repository.register("user", "email@test.com", "pass")

        assertTrue(result is AuthResult.Error)
        assertEquals("User already exists", (result as AuthResult.Error).message)
    }

    @Test
    fun `logout should clear token`() {
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
