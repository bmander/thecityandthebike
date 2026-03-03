package com.thecityandthebike.ui.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application
) : ViewModel() {

    private val androidId: String? = try {
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
    } catch (_: Exception) {
        null
    }

    private val _state = MutableStateFlow(
        AuthState(
            isLoggedIn = authRepository.isLoggedIn.value,
            registrationSuccess = savedStateHandle.get<Boolean>(REGISTRATION_SUCCESS_KEY) ?: false
        )
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (!loggedIn && _state.value.isLoggedIn) {
                    _state.value = _state.value.copy(
                        isLoggedIn = false,
                        error = "Session expired. Please sign in again."
                    )
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedPassword = password.trim()
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(trimmedUsername, trimmedPassword, androidId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.error.displayMessage)
                }
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            val trimmedUsername = username.trim()
            val trimmedPassword = password.trim()
            _state.value = _state.value.copy(isLoading = true, error = null, registrationSuccess = false)
            savedStateHandle[REGISTRATION_SUCCESS_KEY] = false
            when (val result = authRepository.register(trimmedUsername, trimmedPassword, androidId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, registrationSuccess = true)
                    savedStateHandle[REGISTRATION_SUCCESS_KEY] = true
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.error.displayMessage)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = AuthState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearRegistrationSuccess() {
        _state.value = _state.value.copy(registrationSuccess = false)
        savedStateHandle[REGISTRATION_SUCCESS_KEY] = false
    }

    companion object {
        private const val REGISTRATION_SUCCESS_KEY = "registrationSuccess"
    }
}
