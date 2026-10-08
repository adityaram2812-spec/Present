package com.adityaram.present.ui.temporary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TemporaryLectureType
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.domain.repository.TemporaryLectureRepository
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TemporaryLectureUiModel(
    val entity: TemporaryLecture,
    val subjectName: String,
    val dateText: String, // e.g. "15 Oct"
    val timeText: String, // e.g. "10:00 - 11:00"
    val resolvesToTargetName: String? = null // For replacement target
)

data class TemporaryLecturesUiState(
    val isLoading: Boolean = false,
    val temporaryLectures: List<TemporaryLectureUiModel> = emptyList(),
    val availableSubjects: List<Subject> = emptyList(),
    val allTimetableEntries: List<TimetableEntry> = emptyList() // For replacements matching
)

class TemporaryLecturesViewModel(
    private val temporaryLectureRepository: TemporaryLectureRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private fun formatTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    val uiState: StateFlow<TemporaryLecturesUiState> = combine(
        temporaryLectureRepository.getAllTemporaryLectures(),
        timetableRepository.getAllSubjects(),
        timetableRepository.getAllEntries()
    ) { lectures, subjects, entries ->
        
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        val currentEpoch = LocalDate.now().toEpochDay()
        
        // Let's filter out things that are way too old (e.g., > 14 days ago) or we can just show everything.
        // Usually, cleanup logic can handle old entries.
        val upcomingOrRecent = lectures
            .filter { (it.dateEpochDays + 7) >= currentEpoch } // Keep last week and all future
            .sortedWith(compareBy({ it.dateEpochDays }, { it.startTime }))
        
        val uiModels = upcomingOrRecent.map { lecture ->
            val sub = subjects.find { it.id == lecture.subjectId }
            val dateStr = LocalDate.ofEpochDay(lecture.dateEpochDays).format(formatter)
            val timeStr = "${formatTime(lecture.startTime)} - ${formatTime(lecture.endTime)}"
            
            var targetName: String? = null
            if (lecture.type == TemporaryLectureType.REPLACEMENT && lecture.replacesEntryId != null) {
                val targetEntry = entries.find { it.id == lecture.replacesEntryId }
                if (targetEntry != null) {
                    val targetSub = subjects.find { it.id == targetEntry.subjectId }
                    targetName = targetSub?.name ?: "scheduled class"
                }
            }
            
            TemporaryLectureUiModel(
                entity = lecture,
                subjectName = sub?.name ?: "Unknown Subject",
                dateText = dateStr,
                timeText = timeStr,
                resolvesToTargetName = targetName
            )
        }
        
        TemporaryLecturesUiState(
            isLoading = false,
            temporaryLectures = uiModels,
            availableSubjects = subjects,
            allTimetableEntries = entries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TemporaryLecturesUiState(isLoading = true))

    fun deleteTemporaryLecture(lecture: TemporaryLecture) {
        viewModelScope.launch {
            temporaryLectureRepository.deleteTemporaryLecture(lecture)
        }
    }

    fun addTemporaryLecture(
        dateEpochDays: Long,
        startTime: Int,
        endTime: Int,
        subjectId: Long,
        teacher: String?,
        room: String?,
        type: TemporaryLectureType,
        replacesEntryId: Long?
    ) {
        if (endTime <= startTime) return // Basic validation handled by UI mostly, safety check here
        
        viewModelScope.launch {
            temporaryLectureRepository.saveTemporaryLecture(
                TemporaryLecture(
                    dateEpochDays = dateEpochDays,
                    startTime = startTime,
                    endTime = endTime,
                    subjectId = subjectId,
                    teacher = teacher,
                    room = room,
                    type = type,
                    replacesEntryId = replacesEntryId
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PresentApplication
                return TemporaryLecturesViewModel(
                    application.container.temporaryLectureRepository,
                    application.container.timetableRepository
                ) as T
            }
        }
    }
}
