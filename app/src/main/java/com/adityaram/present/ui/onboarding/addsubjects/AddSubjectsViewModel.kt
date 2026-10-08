package com.adityaram.present.ui.onboarding.addsubjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.Subject
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddSubjectsViewModel(
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = timetableRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        
        // Prevent duplicate string entries
        val exists = subjects.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) return

        viewModelScope.launch {
            timetableRepository.insertSubject(
                Subject(
                    name = trimmed,
                    teacher = null,
                    minimumAttendance = 75f, // default
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            timetableRepository.deleteSubjectSafely(subject.id)
        }
    }

    // Keep it simple for manual entry. Edit would just be deleting and adding, or doing an update.
    fun editSubject(subject: Subject, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == subject.name) return
        viewModelScope.launch {
            timetableRepository.updateSubject(subject.copy(name = trimmed))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                assert(application is PresentApplication)
                val appContainer = (application as PresentApplication).container
                return AddSubjectsViewModel(
                    timetableRepository = appContainer.timetableRepository
                ) as T
            }
        }
    }
}
