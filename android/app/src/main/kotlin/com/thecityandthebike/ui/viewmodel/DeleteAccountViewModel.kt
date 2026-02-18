package com.thecityandthebike.ui.viewmodel

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

data class DeleteAccountState(
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true, error = null)
            when (val result = authRepository.deleteAccount()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isDeleting = false, isDeleted = true)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = result.error.displayMessage
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
