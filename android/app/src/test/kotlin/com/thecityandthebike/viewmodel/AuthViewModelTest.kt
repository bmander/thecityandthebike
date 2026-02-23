package com.thecityandthebike.viewmodel

import android.app.Application
import android.content.ContentResolver
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.repository.AuthRepository
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiService: ApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    private lateinit var application: Application
    private lateinit var viewModel: AuthViewModel
    private val isLoggedInFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiService = mockk(relaxed = true)
        tokenManager = mockk(relaxed = true)
        application = mockk(relaxed = true)
        every { tokenManager.isLoggedIn } returns isLoggedInFlow
        every { tokenManager.hasToken() } returns false
        authRepository = AuthRepository(apiService, tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should not be logged in when no token`() {
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)
        assertFalse(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `initial state should be logged in when token exists`() {
        isLoggedInFlow.value = true
        every { tokenManager.hasToken() } returns true
        authRepository = AuthRepository(apiService, tokenManager)
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)
        assertTrue(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `login success should update state to logged in`() = runTest {
        val tokenResponse = com.thecityandthebike.data.model.dto.TokenResponse(
            accessToken = "token", refreshToken = "refresh"
        )
        coEvery { apiService.login(any()) } returns retrofit2.Response.success(tokenResponse)
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.login("user", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoggedIn)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `login failure should set error state`() = runTest {
        coEvery { apiService.login(any()) } returns retrofit2.Response.error(
            401, "{}".toResponseBody()
        )
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.login("user", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoggedIn)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Invalid credentials", viewModel.state.value.error)
    }

    @Test
    fun `register success should set registration success state`() = runTest {
        coEvery { apiService.register(any()) } returns retrofit2.Response.success(
            com.thecityandthebike.data.model.dto.MessageResponse(msg = "User created")
        )
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.register("user", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.registrationSuccess)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `register failure should set error state`() = runTest {
        coEvery { apiService.register(any()) } returns retrofit2.Response.error(
            409, "{}".toResponseBody()
        )
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.register("user", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.registrationSuccess)
        assertEquals("Registration failed", viewModel.state.value.error)
    }

    @Test
    fun `logout should clear logged in state`() = runTest {
        isLoggedInFlow.value = true
        every { tokenManager.hasToken() } returns true
        authRepository = AuthRepository(apiService, tokenManager)
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { apiService.login(any()) } returns retrofit2.Response.error(
            401, "{}".toResponseBody()
        )
        viewModel = AuthViewModel(authRepository, SavedStateHandle(), application)

        viewModel.login("user", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}
