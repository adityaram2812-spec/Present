package com.adityaram.present.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.OpeningBalance
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.data.createOpeningBalancesJson
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OpeningBalanceUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val balances: Map<Long, OpeningBalance> = emptyMap()
)

class OpeningBalanceViewModel(
    private val timetableRepository: TimetableRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _balances = MutableStateFlow<Map<Long, OpeningBalance>>(emptyMap())

    val uiState: StateFlow<OpeningBalanceUiState> = combine(
        timetableRepository.getAllSubjects(),
        _balances
    ) { subjects, balances ->
        OpeningBalanceUiState(
            isLoading = false,
            subjects = subjects,
            balances = balances
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OpeningBalanceUiState(isLoading = true))

    fun updateBalance(subjectId: Long, present: Int, absent: Int) {
        val current = _balances.value.toMutableMap()
        current[subjectId] = OpeningBalance(present, absent)
        _balances.value = current
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val json = createOpeningBalancesJson(_balances.value)
            userPreferencesRepository.setOpeningBalances(json)
            userPreferencesRepository.setTrackingStartDate(System.currentTimeMillis())
            userPreferencesRepository.setOnboardingCompleted(true)
            onSuccess()
        }
    }

    fun startFresh(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            onSuccess()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                assert(application is PresentApplication)
                val appContainer = (application as PresentApplication).container
                return OpeningBalanceViewModel(
                    timetableRepository = appContainer.timetableRepository,
                    userPreferencesRepository = appContainer.userPreferencesRepository
                ) as T
            }
        }
    }
}
