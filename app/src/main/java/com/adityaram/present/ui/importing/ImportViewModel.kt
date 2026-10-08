package com.adityaram.present.ui.importing

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adityaram.present.data.import.MlKitTimetableParser
import com.adityaram.present.data.import.SpreadsheetTimetableParser
import com.adityaram.present.domain.import.ParsedTimetableClass
import com.adityaram.present.domain.import.TimetableImportServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

import com.adityaram.present.domain.import.DocumentLayoutData

sealed class ImportState {
    object Idle : ImportState()
    object Parsing : ImportState()
    data class GroupSelection(val layoutData: DocumentLayoutData, val groups: List<String>) : ImportState()
    data class Review(val classes: List<ParsedTimetableClass>) : ImportState()
    data class ConfirmOverwrite(val classes: List<ParsedTimetableClass>) : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(
    private val importService: TimetableImportServiceImpl,
    private val spreadsheetParser: SpreadsheetTimetableParser,
    private val ocrParser: MlKitTimetableParser,
    private val aiParser: com.adityaram.present.data.import.AiTimetableParser,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportState>(ImportState.Idle)
    val uiState: StateFlow<ImportState> = _uiState

    private var currentParser: com.adityaram.present.domain.import.TimetableParser? = null

    fun parseUri(uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            _uiState.value = ImportState.Parsing
            android.util.Log.d("PresentImport", "IMPORT_IMAGE_URI_READY: Parsing MIME $mimeType")
            try {
                // Copy URI to a temporary file correctly maintaining format reliably
                val ext = when {
                    mimeType?.contains("pdf") == true -> "pdf"
                    mimeType?.contains("spreadsheet") == true || mimeType?.contains("excel") == true -> "xlsx"
                    mimeType?.contains("csv") == true -> "csv"
                    mimeType?.contains("json") == true -> "json"
                    else -> "jpg"
                }

                val tempFile = File(context.cacheDir, "import_temp.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Failed to open file stream")
                
                if (ext == "json") {
                    currentParser = aiParser
                    val jsonContent = tempFile.readText()
                    val aiData = try {
                        com.google.gson.Gson().fromJson(jsonContent, com.adityaram.present.data.import.AiBackendResponse::class.java)
                    } catch (e: Exception) {
                        throw Exception("Malformed JSON structure.")
                    }
                    
                    if (aiData.classes == null) {
                        throw Exception("Invalid timetable JSON format.")
                    }
                    
                    val layoutData = DocumentLayoutData(
                        detectedGroups = aiData.groupsFound ?: emptyList(),
                        aiResponseJson = jsonContent
                    )
                    if (layoutData.detectedGroups.isNotEmpty()) {
                        _uiState.value = ImportState.GroupSelection(layoutData, layoutData.detectedGroups)
                    } else {
                        confirmGroupSelection(layoutData, null, isFullTimetable = true, parser = currentParser)
                    }
                    return@launch
                }

                val parser = if (ext == "xlsx" || ext == "csv") spreadsheetParser else aiParser
                currentParser = parser
                
                val result = parser.analyzeDocument(tempFile)
                result.fold(
                    onSuccess = { layoutData ->
                        if (layoutData.detectedGroups.isNotEmpty()) {
                            _uiState.value = ImportState.GroupSelection(layoutData, layoutData.detectedGroups)
                        } else {
                            // If no groups detected, proceed to extract all
                            confirmGroupSelection(layoutData, null, isFullTimetable = true, parser = parser)
                        }
                    },
                    onFailure = { e ->
                        println("Extraction analysis failed: ${e.message}")
                        _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
                    }
                )
            } catch (e: Exception) {
                println("Extraction exception: ${e.localizedMessage}")
                _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
            }
        }
    }

    fun confirmGroupSelection(layoutData: DocumentLayoutData, selectedGroup: String?, isFullTimetable: Boolean, parser: com.adityaram.present.domain.import.TimetableParser? = null) {
        viewModelScope.launch {
            _uiState.value = ImportState.Parsing
            try {
                val activeParser = parser ?: currentParser ?: throw Exception("No parser active")
                val result = activeParser.extractClasses(layoutData, selectedGroup, isFullTimetable)
                
                result.fold(
                    onSuccess = { parsed ->
                        println("DEBUG FLOW: successfully parsed classes = ${parsed.classes.size}")
                        if (parsed.classes.isEmpty()) {
                            _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
                        } else {
                            println("DEBUG FLOW: classes mapped to Review UI = ${parsed.classes.size}")
                            val debugPractical = parsed.classes.find { it.startTime == 680 }
                            if (debugPractical != null) {
                                println("DEBUG FLOW ViewModel: Passing practical to UI Review State: ${debugPractical.subjectName} Start=${debugPractical.startTime} End=${debugPractical.endTime}")
                            }
                            _uiState.value = ImportState.Review(parsed.classes)
                        }
                    },
                    onFailure = { e ->
                        println("DEBUG FLOW: Parsed failed with error: ${e.message}")
                        _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
                    }
                )
            } catch (e: Exception) {
                println("Extraction exception: ${e.localizedMessage}")
                _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
            }
        }
    }

    fun updateClass(updatedClass: ParsedTimetableClass) {
        val currentState = _uiState.value
        if (currentState is ImportState.Review) {
            val newList = currentState.classes.map {
                if (it.id == updatedClass.id) updatedClass else it
            }
            _uiState.value = ImportState.Review(newList)
        }
    }

    fun deleteClass(classId: String) {
        val currentState = _uiState.value
        if (currentState is ImportState.Review) {
            val newList = currentState.classes.filter { it.id != classId }
            _uiState.value = ImportState.Review(newList)
        }
    }
    
    fun confirmImport() {
        val currentState = _uiState.value
        if (currentState is ImportState.Review) {
            println("DEBUG FLOW: Classes submitted for saving (from Review UI) = ${currentState.classes.size}")
            viewModelScope.launch {
                val hasTimetable = importService.hasExistingTimetable().first()
                if (hasTimetable) {
                    _uiState.value = ImportState.ConfirmOverwrite(currentState.classes)
                } else {
                    _uiState.value = ImportState.Parsing // Show loading overlay
                    importService.confirmImport(com.adityaram.present.domain.import.ParsedTimetable(currentState.classes), false)
                    _uiState.value = ImportState.Success
                }
            }
        }
    }

    fun executeTimetableReplacement(classes: List<ParsedTimetableClass>) {
        viewModelScope.launch {
            _uiState.value = ImportState.Parsing
            importService.confirmImport(com.adityaram.present.domain.import.ParsedTimetable(classes), true)
            _uiState.value = ImportState.Success
        }
    }
    
    fun cancelReplacement() {
        val currentState = _uiState.value
        if (currentState is ImportState.ConfirmOverwrite) {
            _uiState.value = ImportState.Review(currentState.classes)
        }
    }

    fun resetState() {
        _uiState.value = ImportState.Idle
    }

    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.NONE)
    fun evaluateParserForDevelopment(
        parser: com.adityaram.present.domain.import.TimetableParser,
        dummyFile: File
    ) {
        viewModelScope.launch {
            _uiState.value = ImportState.Parsing
            currentParser = parser
            val result = parser.analyzeDocument(dummyFile)
            result.fold(
                onSuccess = { layoutData ->
                    if (layoutData.detectedGroups.isNotEmpty()) {
                        _uiState.value = ImportState.GroupSelection(layoutData, layoutData.detectedGroups)
                    } else {
                        confirmGroupSelection(layoutData, null, isFullTimetable = true, parser = parser)
                    }
                },
                onFailure = { e ->
                    println("Extraction evaluation failed: ${e.message}")
                    _uiState.value = ImportState.Error("We couldn't extract the timetable right now. Please try again later.")
                }
            )
        }
    }

    companion object {
        val Factory: androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = com.adityaram.present.data.AppDatabase.getDatabase(application)
                val repository = com.adityaram.present.domain.repository.TimetableRepository(
                    database = db,
                    subjectDao = db.subjectDao(),
                    timetableEntryDao = db.timetableEntryDao(),
                    attendanceRecordDao = db.attendanceRecordDao()
                )
                
                return ImportViewModel(
                    TimetableImportServiceImpl(repository),
                    SpreadsheetTimetableParser(),
                    MlKitTimetableParser(application.applicationContext),
                    com.adityaram.present.data.import.AiTimetableParser(com.adityaram.present.data.import.BackendExtractionProvider()),
                    application.applicationContext
                ) as T
            }
        }
    }
}
