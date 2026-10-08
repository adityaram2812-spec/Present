package com.adityaram.present.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.ceil
import kotlin.math.floor
import com.adityaram.present.data.parseOpeningBalances

data class PlannerSubjectModel(
    val subject: Subject,
    val presentCount: Int,
    val absentCount: Int,
    val targetRequirement: Float
) {
    val totalCount: Int get() = presentCount + absentCount
    val percentage: Float? get() = if (totalCount > 0) presentCount.toFloat() / totalCount.toFloat() else null
}

data class PlannerUiState(
    val isLoading: Boolean = true,
    val subjects: List<PlannerSubjectModel> = emptyList(),
    val selectedSubjectId: Long? = null,
    val simulatedAttends: Int = 0,
    val simulatedSkips: Int = 0
) {
    val selectedSubject: PlannerSubjectModel? get() = subjects.find { it.subject.id == selectedSubjectId }

    val projectedPresent: Int get() = (selectedSubject?.presentCount ?: 0) + simulatedAttends
    val projectedAbsent: Int get() = (selectedSubject?.absentCount ?: 0) + simulatedSkips
    val projectedTotal: Int get() = projectedPresent + projectedAbsent
    val projectedPercentage: Float? get() = if (projectedTotal > 0) projectedPresent.toFloat() / projectedTotal.toFloat() else null

    val targetRequirement: Float get() = selectedSubject?.targetRequirement ?: 0.75f
    
    val contextMessage: String? get() = projectedPercentage?.let { pct ->
        val target = targetRequirement
        val t = projectedTotal
        val p = projectedPresent
        val targetSafe = (target * 100).toInt()

        if (pct >= target) {
            val marginClasses = floor((p - target * t) / target).toInt()
            if (marginClasses > 0) {
                "You can skip $marginClasses ${if (marginClasses == 1) "class" else "classes"} and stay at $targetSafe%."
            } else {
                "You're exactly at your $targetSafe% target."
            }
        } else {
            val needed = ceil((target * t - p) / (1 - target)).toInt()
            "Attend $needed consecutive ${if (needed == 1) "class" else "classes"} to reach $targetSafe%."
        }
    }
}

class PlannerViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    private val _simulatedAttends = MutableStateFlow(0)
    private val _simulatedSkips = MutableStateFlow(0)

    val uiState: StateFlow<PlannerUiState> = combine(
        combine(
            timetableRepository.getAllSubjects(),
            attendanceRepository.getAllRecords(),
            userPreferencesRepository.attendanceRequirement,
            userPreferencesRepository.openingBalances
        ) { subjects, records, req, balancesJson ->
            val balances = parseOpeningBalances(balancesJson)
            listOf<Any>(subjects, records, req, balances)
        },
        combine(
            _selectedSubjectId,
            _simulatedAttends,
            _simulatedSkips,
            ::Triple
        )
    ) { layer1, (selectedId, attends, skips) ->
        @Suppress("UNCHECKED_CAST")
        val subjects = layer1[0] as List<Subject>
        @Suppress("UNCHECKED_CAST")
        val records = layer1[1] as List<com.adityaram.present.data.model.AttendanceRecord>
        val requirement = layer1[2] as Int
        @Suppress("UNCHECKED_CAST")
        val balances = layer1[3] as Map<Long, com.adityaram.present.data.OpeningBalance>

        val target = requirement / 100f
        
        val models = subjects.map { subject ->
            val ob = balances[subject.id]
            val subjRecords = records.filter { it.subjectId == subject.id && it.status != AttendanceStatus.CANCELLED }
            val p = subjRecords.count { it.status == AttendanceStatus.PRESENT } + (ob?.present ?: 0)
            val a = subjRecords.count { it.status == AttendanceStatus.ABSENT } + (ob?.absent ?: 0)
            PlannerSubjectModel(
                subject = subject,
                presentCount = p,
                absentCount = a,
                targetRequirement = target
            )
        }.sortedBy { it.subject.name }

        // If no subject is explicitly selected by user, default to the first one safely
        val activeId = selectedId ?: models.firstOrNull()?.subject?.id

        PlannerUiState(
            isLoading = false,
            subjects = models,
            selectedSubjectId = activeId,
            simulatedAttends = attends,
            simulatedSkips = skips
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlannerUiState(isLoading = true))

    fun selectSubject(id: Long) {
        _selectedSubjectId.value = id
        _simulatedAttends.value = 0
        _simulatedSkips.value = 0
    }

    fun updateAttend(increment: Int) {
        _simulatedAttends.value = maxOf(0, _simulatedAttends.value + increment)
    }

    fun updateSkip(increment: Int) {
        _simulatedSkips.value = maxOf(0, _simulatedSkips.value + increment)
    }

    fun resetSimulation() {
        _simulatedAttends.value = 0
        _simulatedSkips.value = 0
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                assert(application is PresentApplication)
                val appContainer = (application as PresentApplication).container
                return PlannerViewModel(
                    attendanceRepository = appContainer.attendanceRepository,
                    timetableRepository = appContainer.timetableRepository,
                    userPreferencesRepository = appContainer.userPreferencesRepository
                ) as T
            }
        }
    }
}
