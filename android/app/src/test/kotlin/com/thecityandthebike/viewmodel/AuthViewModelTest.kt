package com.thecityandthebike.viewmodel

import com.thecityandthebike.data.repository.AuthRepository
import com.thecityandthebike.data.repository.AuthResult
import com.thecityandthebike.ui.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        every { authRepository.isLoggedIn() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should not be logged in when no token`() {
        viewModel = AuthViewModel(authRepository)
        assertFalse(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `initial state should be logged in when token exists`() {
        every { authRepository.isLoggedIn() } returns true
        viewModel = AuthViewModel(authRepository)
        assertTrue(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `login success should update state to logged in`() = runTest {
        coEvery { authRepository.login("user", "pass") } returns AuthResult.Success(Unit)
        viewModel = AuthViewModel(authRepository)

        viewModel.login("user", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoggedIn)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `login failure should set error state`() = runTest {
        coEvery { authRepository.login("user", "wrong") } returns AuthResult.Error("Invalid credentials")
        viewModel = AuthViewModel(authRepository)

        viewModel.login("user", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoggedIn)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Invalid credentials", viewModel.state.value.error)
    }

    @Test
    fun `register success should set registration success state`() = runTest {
        coEvery { authRepository.register("user", "email@test.com", "pass") } returns AuthResult.Success(Unit)
        viewModel = AuthViewModel(authRepository)

        viewModel.register("user", "email@test.com", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.registrationSuccess)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `register failure should set error state`() = runTest {
        coEvery { authRepository.register("user", "email@test.com", "pass") } returns AuthResult.Error("User already exists")
        viewModel = AuthViewModel(authRepository)

        viewModel.register("user", "email@test.com", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.registrationSuccess)
        assertEquals("User already exists", viewModel.state.value.error)
    }

    @Test
    fun `logout should clear logged in state`() {
        every { authRepository.isLoggedIn() } returns true
        every { authRepository.logout() } returns Unit
        viewModel = AuthViewModel(authRepository)

        viewModel.logout()

        assertFalse(viewModel.state.value.isLoggedIn)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { authRepository.login("user", "wrong") } returns AuthResult.Error("Error")
        viewModel = AuthViewModel(authRepository)

        viewModel.login("user", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}
