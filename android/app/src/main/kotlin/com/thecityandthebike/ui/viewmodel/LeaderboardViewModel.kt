package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.LeaderboardEntry
import com.thecityandthebike.data.repository.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeaderboardPeriod(val apiValue: String, val displayName: String) {
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
    ALL_TIME("all_time", "All Time")
}

data class LeaderboardState(
    val isLoading: Boolean = false,
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.WEEKLY,
    val entries: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun selectPeriod(period: LeaderboardPeriod) {
        _state.value = _state.value.copy(selectedPeriod = period)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = leaderboardRepository.getLeaderboard(_state.value.selectedPeriod.apiValue)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        entries = result.data.entries
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.error.displayMessageOrNull
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
