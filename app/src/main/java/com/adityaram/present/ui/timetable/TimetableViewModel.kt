package com.adityaram.present.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
data class TimetableClassUiModel(
    val entry: TimetableEntry,
    val subjectName: String,
    val attendanceStatus: AttendanceStatus?, 
    val isNext: Boolean = false,
    val isCancelled: Boolean = false,
    val isExtraClass: Boolean = false
)

data class TimetableUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val classes: List<TimetableClassUiModel> = emptyList(),
    val weekDates: List<LocalDate> = emptyList(),
    val subjects: List<com.adityaram.present.data.model.Subject> = emptyList()
)

class TimetableViewModel(
    private val timetableRepository: TimetableRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TimetableUiState> = combine(
        _selectedDate,
        timetableRepository.getAllEntries(),
        timetableRepository.getAllSubjects(),
        attendanceRepository.getAllRecords()
    ) { date, entries, subjects, records ->
        
        // Generate week dates (Mon to Sun for the current week of selectedDate)
        val dayOfWeek = date.dayOfWeek.value
        val monday = date.minusDays((dayOfWeek - 1).toLong())
        val weekDates = (0..6).map { monday.plusDays(it.toLong()) }

        val startOfDayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Filter entries for selected day
        val dayEntries = entries.filter { entry ->
            if (entry.isRecurring) {
                entry.dayOfWeek == date.dayOfWeek.value
            } else {
                entry.specificDateMillis == startOfDayMillis
            }
        }.sortedBy { it.startTime }

        val endOfDayMillis = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val dateRecords = records.filter { it.date in startOfDayMillis until endOfDayMillis }

        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val isToday = date == LocalDate.now()

        // Find the "Next" class if today
        var nextClassId: Long? = null
        if (isToday) {
            nextClassId = dayEntries.firstOrNull { it.startTime > currentMinutes }?.id
        }

        val classes = dayEntries.map { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            val record = dateRecords.find { 
                it.subjectId == entry.subjectId && it.startTime == entry.startTime 
            }
            
            TimetableClassUiModel(
                entry = entry,
                subjectName = subject?.name ?: "Unknown Subject",
                attendanceStatus = record?.status,
                isNext = entry.id == nextClassId,
                isCancelled = record?.status == AttendanceStatus.CANCELLED,
                isExtraClass = !entry.isRecurring
            )
        }

        TimetableUiState(
            selectedDate = date,
            classes = classes,
            weekDates = weekDates,
            subjects = subjects
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState(
            weekDates = (0..6).map { LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong()).plusDays(it.toLong()) }
        )
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.update { date }
    }

    fun addClass(
        subjectName: String,
        dayOfWeek: Int,
        startTime: Int, // minutes from midnight
        endTime: Int,
        room: String?,
        teacher: String?,
        isRecurring: Boolean,
        date: LocalDate?
    ) {
        viewModelScope.launch {
            val normalizedName = subjectName.trim().replace("\\s+".toRegex(), " ")
            val existingSubjects = uiState.value.subjects
            var resolvedSubjectId = existingSubjects.find { it.name.equals(normalizedName, ignoreCase = true) }?.id
            
            if (resolvedSubjectId == null) {
                val newSubject = Subject(
                    name = normalizedName,
                    teacher = teacher?.takeIf { it.isNotBlank() },
                    minimumAttendance = 75f,
                    createdAt = System.currentTimeMillis()
                )
                resolvedSubjectId = timetableRepository.insertSubject(newSubject)
            }

            val specificDateMillis = if (!isRecurring && date != null) {
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else null
            
            val newEntry = TimetableEntry(
                subjectId = resolvedSubjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room?.takeIf { it.isNotBlank() },
                teacher = teacher?.takeIf { it.isNotBlank() },
                isRecurring = isRecurring,
                specificDateMillis = specificDateMillis
            )
            timetableRepository.insertEntries(listOf(newEntry))
        }
    }

    fun updateClass(
        entryId: Long,
        subjectName: String,
        dayOfWeek: Int,
        startTime: Int, // minutes from midnight
        endTime: Int,
        room: String?,
        teacher: String?,
        isRecurring: Boolean,
        date: LocalDate?
    ) {
        viewModelScope.launch {
            val normalizedName = subjectName.trim().replace("\\s+".toRegex(), " ")
            val existingSubjects = uiState.value.subjects
            var resolvedSubjectId = existingSubjects.find { it.name.equals(normalizedName, ignoreCase = true) }?.id
            
            if (resolvedSubjectId == null) {
                val newSubject = Subject(
                    name = normalizedName,
                    teacher = teacher?.takeIf { it.isNotBlank() },
                    minimumAttendance = 75f,
                    createdAt = System.currentTimeMillis()
                )
                resolvedSubjectId = timetableRepository.insertSubject(newSubject)
            }

            val specificDateMillis = if (!isRecurring && date != null) {
                date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else null
            
            val updatedEntry = TimetableEntry(
                id = entryId,
                subjectId = resolvedSubjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room?.takeIf { it.isNotBlank() },
                teacher = teacher?.takeIf { it.isNotBlank() },
                isRecurring = isRecurring,
                specificDateMillis = specificDateMillis
            )
            timetableRepository.updateEntry(updatedEntry)
        }
    }

    fun duplicateClass(cls: TimetableClassUiModel) {
        viewModelScope.launch {
            val newEntry = cls.entry.copy(id = 0)
            timetableRepository.insertEntries(listOf(newEntry))
        }
    }

    fun cancelClassOccurrence(cls: TimetableClassUiModel, date: LocalDate) {
        viewModelScope.launch {
            val startOfDayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            attendanceRepository.cancelOccurrence(cls.entry.subjectId, startOfDayMillis, cls.entry.startTime)
        }
    }

    fun restoreClassOccurrence(cls: TimetableClassUiModel, date: LocalDate) {
        viewModelScope.launch {
            val startOfDayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            attendanceRepository.restoreOccurrence(cls.entry.subjectId, startOfDayMillis, cls.entry.startTime)
        }
    }

    fun deleteClass(cls: TimetableClassUiModel) {
        viewModelScope.launch {
            timetableRepository.deleteEntry(cls.entry)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as PresentApplication)
                TimetableViewModel(
                    timetableRepository = application.container.timetableRepository,
                    attendanceRepository = application.container.attendanceRepository
                )
            }
        }
    }
}
