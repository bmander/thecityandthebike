package com.thecityandthebike.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.integration.IntegrationTestUtils.createApiService
import com.thecityandthebike.integration.IntegrationTestUtils.generateUniqueEmail
import com.thecityandthebike.integration.IntegrationTestUtils.generateUniqueUsername
import com.thecityandthebike.integration.IntegrationTestUtils.registerTestUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthIntegrationTest {

    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        apiService = createApiService()
    }

    @Test
    fun register_withValidCredentials_succeeds() = runBlocking {
        val username = generateUniqueUsername()
        val email = generateUniqueEmail()
        val password = "testpassword123"

        val response = apiService.register(RegisterRequest(username, email, password))

        assertTrue("Registration should succeed", response.isSuccessful)
        assertEquals(201, response.code())
        assertNotNull(response.body())
        assertEquals("User created", response.body()?.msg)
    }

    @Test
    fun register_withDuplicateUsername_fails() = runBlocking {
        val username = generateUniqueUsername()
        val email1 = generateUniqueEmail()
        val email2 = generateUniqueEmail()
        val password = "testpassword123"

        // First registration should succeed
        val firstResponse = apiService.register(RegisterRequest(username, email1, password))
        assertTrue("First registration should succeed", firstResponse.isSuccessful)

        // Second registration with same username should fail
        val secondResponse = apiService.register(RegisterRequest(username, email2, password))
        assertFalse("Second registration with duplicate username should fail", secondResponse.isSuccessful)
        assertEquals(409, secondResponse.code())
    }

    @Test
    fun register_withDuplicateEmail_fails() = runBlocking {
        val username1 = generateUniqueUsername()
        val username2 = generateUniqueUsername()
        val email = generateUniqueEmail()
        val password = "testpassword123"

        // First registration should succeed
        val firstResponse = apiService.register(RegisterRequest(username1, email, password))
        assertTrue("First registration should succeed", firstResponse.isSuccessful)

        // Second registration with same email should fail
        val secondResponse = apiService.register(RegisterRequest(username2, email, password))
        assertFalse("Second registration with duplicate email should fail", secondResponse.isSuccessful)
        assertEquals(409, secondResponse.code())
    }

    @Test
    fun login_afterRegistration_returnsValidToken() = runBlocking {
        val username = generateUniqueUsername()
        val email = generateUniqueEmail()
        val password = "testpassword123"

        // Register user
        val registerResponse = apiService.register(RegisterRequest(username, email, password))
        assertTrue("Registration should succeed", registerResponse.isSuccessful)

        // Login
        val loginResponse = apiService.login(LoginRequest(username, password))
        assertTrue("Login should succeed", loginResponse.isSuccessful)
        assertEquals(200, loginResponse.code())

        val tokenResponse = loginResponse.body()
        assertNotNull("Token response should not be null", tokenResponse)
        assertNotNull("Access token should not be null", tokenResponse?.accessToken)
        assertTrue("Access token should not be empty", tokenResponse?.accessToken?.isNotEmpty() == true)
        assertEquals("bearer", tokenResponse?.tokenType)
    }

    @Test
    fun login_withInvalidPassword_fails() = runBlocking {
        val username = generateUniqueUsername()
        val email = generateUniqueEmail()
        val correctPassword = "testpassword123"
        val wrongPassword = "wrongpassword"

        // Register user
        val registerResponse = apiService.register(RegisterRequest(username, email, correctPassword))
        assertTrue("Registration should succeed", registerResponse.isSuccessful)

        // Try login with wrong password
        val loginResponse = apiService.login(LoginRequest(username, wrongPassword))
        assertFalse("Login with wrong password should fail", loginResponse.isSuccessful)
        assertEquals(401, loginResponse.code())
    }

    @Test
    fun login_withNonexistentUser_fails() = runBlocking {
        val username = "nonexistent_${generateUniqueUsername()}"
        val password = "somepassword"

        val loginResponse = apiService.login(LoginRequest(username, password))
        assertFalse("Login with nonexistent user should fail", loginResponse.isSuccessful)
        assertEquals(401, loginResponse.code())
    }
}
