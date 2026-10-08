package com.adityaram.present.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarSessionUiModel(
    val record: AttendanceRecord,
    val subjectName: String,
    val teacher: String?,
    val room: String?,
    val startTime: Int
)

data class CalendarDayUiModel(
    val date: LocalDate,
    val presentCount: Int,
    val absentCount: Int,
    val status: DayStatus,
    val isToday: Boolean,
    val isSelected: Boolean,
    val inCurrentMonth: Boolean
) {
    val totalCount = presentCount + absentCount
    val percentage = if (totalCount > 0) presentCount.toFloat() / totalCount.toFloat() else 0f
}

enum class DayStatus {
    NONE, PRESENT_ONLY, ABSENT_ONLY, MIXED
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val days: List<CalendarDayUiModel> = emptyList(),
    val monthAverage: Float? = null,
    val selectedSessions: List<CalendarSessionUiModel> = emptyList()
)

class CalendarViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        attendanceRepository.getAllRecords(),
        timetableRepository.getAllSubjects(),
        timetableRepository.getAllEntries()
    ) { month, selectedDate, records, subjects, entries ->
        val validRecords = records.filter { it.status != AttendanceStatus.CANCELLED }

        // Process Month grid
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()
        val firstDayOfWeek = startOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val startDate = startOfMonth.minusDays((firstDayOfWeek - 1).toLong())
        val daysInGrid = 42 // 6 weeks

        val days = (0 until daysInGrid).map { i ->
            val d = startDate.plusDays(i.toLong())
            val dStartBase = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dEndBase = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val dRecords = validRecords.filter { it.date in dStartBase until dEndBase }
            val presentCount = dRecords.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = dRecords.count { it.status == AttendanceStatus.ABSENT }
            
            val status = when {
                presentCount > 0 && absentCount == 0 -> DayStatus.PRESENT_ONLY
                absentCount > 0 && presentCount == 0 -> DayStatus.ABSENT_ONLY
                presentCount > 0 && absentCount > 0 -> DayStatus.MIXED
                else -> DayStatus.NONE
            }

            CalendarDayUiModel(
                date = d,
                presentCount = presentCount,
                absentCount = absentCount,
                status = status,
                isToday = d == LocalDate.now(),
                isSelected = d == selectedDate,
                inCurrentMonth = d.month == month.month && d.year == month.year
            )
        }

        // Monthly average calculation
        val monthStartMillis = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndMillis = endOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthRecords = validRecords.filter { it.date in monthStartMillis until monthEndMillis }
        val monthPresent = monthRecords.count { it.status == AttendanceStatus.PRESENT }
        val monthAbsent = monthRecords.count { it.status == AttendanceStatus.ABSENT }
        val monthTotal = monthPresent + monthAbsent
        val monthAverage = if (monthTotal > 0) monthPresent.toFloat() / monthTotal.toFloat() else null

        // Selected Date Sessions
        val selectedStartMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val selectedEndMillis = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val selectedRecords = validRecords.filter { it.date in selectedStartMillis until selectedEndMillis }
        
        val sessions = selectedRecords.map { record ->
            val subject = subjects.find { it.id == record.subjectId }
            // To find room/teacher from TimetableEntry, match subjectId, dayOfWeek, and startTime
            val dayOfWeek = selectedDate.dayOfWeek.value
            val entry = entries.find { 
                it.subjectId == record.subjectId && 
                it.startTime == record.startTime && 
                it.dayOfWeek == dayOfWeek 
            }
            
            CalendarSessionUiModel(
                record = record,
                subjectName = subject?.name ?: "Unknown Subject",
                teacher = subject?.teacher ?: entry?.teacher,
                room = entry?.room,
                startTime = record.startTime
            )
        }.sortedBy { it.startTime }

        CalendarUiState(
            isLoading = false,
            currentMonth = month,
            selectedDate = selectedDate,
            days = days,
            monthAverage = monthAverage,
            selectedSessions = sessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(isLoading = true)
    )

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun onSelectToday() {
        val today = LocalDate.now()
        _currentMonth.value = YearMonth.now()
        _selectedDate.value = today
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        if (date.month != _currentMonth.value.month) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun toggleAttendanceStatus(record: AttendanceRecord) {
        val newStatus = if (record.status == AttendanceStatus.PRESENT) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
        viewModelScope.launch {
            attendanceRepository.updateRecord(record.copy(status = newStatus))
        }
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
                return CalendarViewModel(
                    attendanceRepository = appContainer.attendanceRepository,
                    timetableRepository = appContainer.timetableRepository
                ) as T
            }
        }
    }
}
