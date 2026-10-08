package com.adityaram.present.ui.subjects

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.data.parseOpeningBalances
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubjectUiModel(
    val subject: Subject,
    val percentage: Float,
    val presentCount: Int,
    val absentCount: Int,
    val targetRequirement: Float,
    val records: List<AttendanceRecord>,
    val todayClasses: List<SubjectTodayClass>,
    val historicalEligibleCount: Int
)

data class SubjectTodayClass(
    val entryId: Long,
    val startTime: Int,
    val recordId: Long?,
    val status: AttendanceStatus?
)

data class SubjectsUiState(
    val isLoading: Boolean = true,
    val subjects: List<SubjectUiModel> = emptyList()
)

class SubjectsViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SubjectsUiState> = combine(
        timetableRepository.getAllSubjects(),
        attendanceRepository.getAllRecords(),
        userPreferencesRepository.attendanceRequirement,
        timetableRepository.getAllEntries(),
        userPreferencesRepository.openingBalances
    ) { subjects, records, requirement, entries, balancesJson ->
        val openingBalances = parseOpeningBalances(balancesJson)
        val date = java.time.LocalDate.now()
        val dayOfWeek = date.dayOfWeek.value
        val startOfDayMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDayMillis = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val models = subjects.map { subject ->
            val ob = openingBalances[subject.id]
            val subjRecords = records.filter { it.subjectId == subject.id }
            val validSubjectRecords = subjRecords.filter { it.status != AttendanceStatus.CANCELLED }
            
            val presentCount = validSubjectRecords.count { it.status == AttendanceStatus.PRESENT } + (ob?.present ?: 0)
            val absentCount = validSubjectRecords.count { it.status == AttendanceStatus.ABSENT } + (ob?.absent ?: 0)
            val totalEligible = presentCount + absentCount
            val percentage = attendanceRepository.calculateAttendanceRatio(presentCount, totalEligible)
            
            val dayEntries = entries.filter { entry ->
                entry.subjectId == subject.id &&
                ((entry.isRecurring && entry.dayOfWeek == dayOfWeek) || 
                 (!entry.isRecurring && entry.specificDateMillis == startOfDayMillis))
            }
            
            val todayClasses = dayEntries.map { entry ->
                val record = records.find { 
                    it.subjectId == subject.id && 
                    it.startTime == entry.startTime && 
                    it.date in startOfDayMillis until endOfDayMillis 
                }
                SubjectTodayClass(
                    entryId = entry.id,
                    startTime = entry.startTime,
                    recordId = record?.id,
                    status = record?.status
                )
            }

            SubjectUiModel(
                subject = subject,
                percentage = percentage,
                presentCount = presentCount,
                absentCount = absentCount,
                targetRequirement = requirement / 100f, // Configured as a consistent 0-1 ratio
                records = subjRecords.sortedByDescending { it.date + (it.startTime * 60000L) }, // Chronological
                todayClasses = todayClasses,
                historicalEligibleCount = totalEligible
            )
        }
        SubjectsUiState(isLoading = false, subjects = models)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectsUiState(isLoading = true)
    )

    fun toggleAttendance(record: AttendanceRecord) {
        val newStatus = if (record.status == AttendanceStatus.PRESENT) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
        viewModelScope.launch {
            attendanceRepository.updateRecord(record.copy(status = newStatus))
        }
    }

    fun markBulkAttendance(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val targetSubjectUi = uiState.value.subjects.find { it.subject.id == subjectId } ?: return@launch
            val validTodayClasses = targetSubjectUi.todayClasses.filter { it.status != AttendanceStatus.CANCELLED }
            
            if (validTodayClasses.isEmpty()) return@launch

            val date = java.time.LocalDate.now()
            val startOfDayMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            validTodayClasses.forEach { cls ->
                if (cls.recordId != null) {
                    val record = attendanceRepository.getAllRecords().first().find { it.id == cls.recordId }
                    if (record != null && record.status != status) {
                        attendanceRepository.updateRecord(record.copy(status = status))
                    }
                } else {
                    attendanceRepository.upsertRecord(subjectId, startOfDayMillis, cls.startTime, status)
                }
            }
        }
    }

    fun editSubject(subjectId: Long, newName: String, newTeacher: String?): Boolean {
        val normalizedName = newName.trim().replace("\\s+".toRegex(), " ")
        if (normalizedName.isEmpty()) return false

        val currentSubjects = uiState.value.subjects
        val existingDuplicate = currentSubjects.find { 
            it.subject.name.equals(normalizedName, ignoreCase = true) && it.subject.id != subjectId 
        }
        if (existingDuplicate != null) return false // Duplicate name rule

        val targetSubjectUi = currentSubjects.find { it.subject.id == subjectId } ?: return false
        val updatedSubject = targetSubjectUi.subject.copy(
            name = normalizedName,
            teacher = newTeacher?.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            timetableRepository.updateSubject(updatedSubject)
        }
        return true
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            timetableRepository.deleteSubjectSafely(subjectId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                assert(application is PresentApplication)
                val appContainer = (application as PresentApplication).container
                return SubjectsViewModel(
                    attendanceRepository = appContainer.attendanceRepository,
                    timetableRepository = appContainer.timetableRepository,
                    userPreferencesRepository = appContainer.userPreferencesRepository
                ) as T
            }
        }
    }
}
