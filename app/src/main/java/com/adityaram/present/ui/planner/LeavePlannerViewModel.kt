package com.adityaram.present.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.LeavePlan
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.HolidayRepository
import com.adityaram.present.domain.repository.LeavePlanRepository
import com.adityaram.present.domain.repository.TemporaryLectureRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LeavePlanModel(
    val entity: LeavePlan,
    val durationDays: Int,
    val affectedLectures: Int
)

data class LeavePlannerUiState(
    val isLoading: Boolean = false,
    val leavePlans: List<LeavePlanModel> = emptyList(),
    val currentAttendance: Float = 1f,
    val totalAffectedLectures: Int = 0,
    val projectedAttendance: Float = 1f
)

class LeavePlannerViewModel(
    private val leavePlanRepository: LeavePlanRepository,
    private val timetableRepository: TimetableRepository,
    private val attendanceRepository: AttendanceRepository,
    private val holidayRepository: HolidayRepository,
    private val temporaryLectureRepository: TemporaryLectureRepository
) : ViewModel() {

    val uiState: StateFlow<LeavePlannerUiState> = combine(
        combine(
            leavePlanRepository.getAllLeavePlans(),
            timetableRepository.getAllEntries(),
            holidayRepository.getAllHolidays()
        ) { plans, entries, holidays -> Triple(plans, entries, holidays) },
        combine(
            attendanceRepository.getAllRecords(),
            timetableRepository.getAllSubjects(),
            temporaryLectureRepository.getAllTemporaryLectures()
        ) { records, subjects, temps -> Triple(records, subjects, temps) }
    ) { (plans, entries, holidays), (records, subjects, temps) ->
        val result = LeavePlannerLogic.calculateProjection(plans, entries, holidays, records, subjects, temps)
        
        LeavePlannerUiState(
            isLoading = false,
            leavePlans = result.planModels,
            currentAttendance = result.currentAttendance,
            totalAffectedLectures = result.totalAffectedLectures,
            projectedAttendance = result.projectedAttendance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LeavePlannerUiState(isLoading = true))

    fun addLeavePlan(startDate: Long, endDate: Long, reason: String?) {
        if (endDate >= startDate) {
            viewModelScope.launch {
                leavePlanRepository.saveLeavePlan(
                    LeavePlan(
                        startDateEpochDays = startDate,
                        endDateEpochDays = endDate,
                        reason = reason
                    )
                )
            }
        }
    }
    
    fun updateLeavePlan(id: Long, startDate: Long, endDate: Long, reason: String?) {
        if (endDate >= startDate) {
            viewModelScope.launch {
                val existing = leavePlanRepository.getLeavePlan(id)
                if (existing != null) {
                    leavePlanRepository.saveLeavePlan(
                        existing.copy(
                            startDateEpochDays = startDate,
                            endDateEpochDays = endDate,
                            reason = reason
                        )
                    )
                }
            }
        }
    }

    fun deleteLeavePlan(plan: LeavePlan) {
        viewModelScope.launch {
            leavePlanRepository.deleteLeavePlan(plan)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PresentApplication
                return LeavePlannerViewModel(
                    application.container.leavePlanRepository,
                    application.container.timetableRepository,
                    application.container.attendanceRepository,
                    application.container.holidayRepository,
                    application.container.temporaryLectureRepository
                ) as T
            }
        }
    }
}
