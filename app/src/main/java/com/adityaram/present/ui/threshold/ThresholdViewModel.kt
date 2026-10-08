package com.adityaram.present.ui.threshold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThresholdViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val thresholdUiState: StateFlow<Int> = userPreferencesRepository.attendanceRequirement
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 75
        )

    fun setThreshold(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAttendanceRequirement(value)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as PresentApplication)
                ThresholdViewModel(application.container.userPreferencesRepository)
            }
        }
    }
}
