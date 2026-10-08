package com.adityaram.present.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoreUiState(
    val activeSubjectCount: Int = 0,
    val themeMode: String = "dark",
    val attendanceRequirement: Int = 75
) {
    val appearanceLabel: String get() = when(themeMode) {
        "light" -> "Light"
        "system" -> "System"
        else -> "Dark"
    }
    val thresholdLabel: String get() = "${attendanceRequirement}%"
}

class MoreViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = combine(
        timetableRepository.getAllSubjects().map { it.size },
        userPreferencesRepository.themeMode,
        userPreferencesRepository.attendanceRequirement
    ) { subjectCount, themeMode, requirement ->
        MoreUiState(
            activeSubjectCount = subjectCount,
            themeMode = themeMode,
            attendanceRequirement = requirement
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoreUiState())

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(false)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                assert(application is PresentApplication)
                val appContainer = (application as PresentApplication).container
                return MoreViewModel(
                    userPreferencesRepository = appContainer.userPreferencesRepository,
                    timetableRepository = appContainer.timetableRepository
                ) as T
            }
        }
    }
}
