package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.repository.AuthRepository
import com.thecityandthebike.data.repository.AuthResult
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState(isLoggedIn = authRepository.isLoggedIn()))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = authRepository.login(username, password)) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, registrationSuccess = false)
            when (val result = authRepository.register(username, email, password)) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, registrationSuccess = true)
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _state.value = AuthState(isLoggedIn = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearRegistrationSuccess() {
        _state.value = _state.value.copy(registrationSuccess = false)
    }
}
