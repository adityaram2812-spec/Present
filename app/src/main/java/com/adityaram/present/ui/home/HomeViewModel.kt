package com.adityaram.present.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.StartupState
import com.adityaram.present.domain.repository.TimetableRepository
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.data.parseOpeningBalances
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ClassUiModel(
    val entryId: Long,
    val subjectId: Long,
    val subjectName: String,
    val startTime: Int,
    val endTime: Int,
    val room: String?,
    val teacher: String?,
    val attendanceRecordId: Long?,
    val attendanceStatus: AttendanceStatus?,
    val isNext: Boolean = false,
    val subjectPercentage: Float = 100f,
    val recoveryClassesNeeded: Int? = null,
    val isAttendanceLow: Boolean = false,
    val isTemporary: Boolean = false,
    val temporaryLabel: String? = null
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val attendanceRequirement: Int = 75,
    val overallPresent: Int = 0,
    val overallAbsent: Int = 0,
    val overallPercentage: Float = 100f,
    val todayClasses: List<ClassUiModel> = emptyList(),
    val hasTimetable: Boolean = true,
    val overallRecoveryClassesNeeded: Int? = null,
    val overallClassesCanMiss: Int? = null,
    val isHoliday: Boolean = false
)

class HomeViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val holidayRepository: com.adityaram.present.domain.repository.HolidayRepository,
    private val temporaryLectureRepository: com.adityaram.present.domain.repository.TemporaryLectureRepository
) : ViewModel() {

    data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastUndoAction: (suspend () -> Unit)? = null

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            val currentDayOfWeek = today.dayOfWeek.value
            val todayStartEpoch = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

            combine(
                combine(
                    userPreferencesRepository.attendanceRequirement,
                    attendanceRepository.getAllRecords(),
                    timetableRepository.getAllSubjects(),
                    temporaryLectureRepository.getAllTemporaryLectures()
                ) { req, recs, subs, temps -> Tuple4(req, recs, subs, temps) },
                combine(
                    timetableRepository.getAllEntries(),
                    userPreferencesRepository.openingBalances,
                    holidayRepository.getAllHolidays()
                ) { entries, balancesJson, holidays -> Triple(entries, balancesJson, holidays) }
            ) { (requirement, records, subjects, temps), (entries, balancesJson, holidays) ->
                val openingBalances = parseOpeningBalances(balancesJson)
                val hasTimetable = entries.isNotEmpty()
                
                val date = java.time.LocalDate.now()
                val todayEpochDate = date.toEpochDay()
                val isHoliday = holidays.any { it.localDateEpochDays == todayEpochDate }
                
                val endOfDayMillis = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                var overallTotal = 0
                val subjectPercentages = mutableMapOf<Long, Float>()
                val subjTotals = mutableMapOf<Long, Int>()
                val subjPresents = mutableMapOf<Long, Int>()
                
                var globalPresentCount = 0
                var globalAbsentCount = 0
                
                subjects.forEach { subject ->
                    val ob = openingBalances[subject.id]
                    val validSubjectRecords = records.filter { it.subjectId == subject.id && it.status != AttendanceStatus.CANCELLED }
                    
                    val presentCount = validSubjectRecords.count { it.status == AttendanceStatus.PRESENT } + (ob?.present ?: 0)
                    val absentCount = validSubjectRecords.count { it.status == AttendanceStatus.ABSENT } + (ob?.absent ?: 0)
                    val totalEligible = presentCount + absentCount
                    
                    globalPresentCount += presentCount
                    globalAbsentCount += absentCount
                    overallTotal += totalEligible
                    
                    subjectPercentages[subject.id] = attendanceRepository.calculateAttendanceRatio(presentCount, totalEligible)
                    subjTotals[subject.id] = totalEligible
                    subjPresents[subject.id] = presentCount
                }
                
                val percentage = attendanceRepository.calculateAttendanceRatio(globalPresentCount, overallTotal)

                val effectiveLectures = com.adityaram.present.domain.schedule.ScheduleResolver.getEffectiveScheduleForDate(
                    dateEpochDays = todayEpochDate,
                    baseEntries = entries,
                    temporaryLectures = temps,
                    holidays = holidays,
                    subjects = subjects
                )
                
                val todayRecords = records.filter { it.date == todayStartEpoch }

                val nowMinutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                var nextFound = false

                val classUiModels = effectiveLectures.map { lecture ->
                    val subject = subjects.find { it.id == lecture.subjectId }
                    val record = todayRecords.find { it.subjectId == lecture.subjectId && it.startTime == lecture.startTime }
                    
                    val isNext = if (!nextFound && lecture.startTime > nowMinutes) {
                        nextFound = true
                        true
                    } else false

                    val subjectPercentage = subjectPercentages[lecture.subjectId] ?: 100f
                    val subjTotal = subjTotals[lecture.subjectId] ?: 0
                    val subjPresent = subjPresents[lecture.subjectId] ?: 0
                    
                    val reqFraction = requirement / 100f
                    val recoveryClassesNeeded = if (subjectPercentage < reqFraction && reqFraction < 1f) {
                        val x = (reqFraction * subjTotal - subjPresent) / (1f - reqFraction)
                        if (x > 0) kotlin.math.ceil(x.toDouble()).toInt() else null
                    } else null

                    ClassUiModel(
                        entryId = lecture.temporaryLecture?.id ?: lecture.baseEntry?.id ?: 0L,
                        subjectId = lecture.subjectId,
                        subjectName = subject?.name ?: "Unknown Subject",
                        startTime = lecture.startTime,
                        endTime = lecture.endTime,
                        room = lecture.temporaryLecture?.room ?: lecture.baseEntry?.room,
                        teacher = lecture.temporaryLecture?.teacher ?: lecture.baseEntry?.teacher ?: subject?.teacher,
                        attendanceRecordId = record?.id,
                        attendanceStatus = record?.status,
                        isNext = isNext,
                        subjectPercentage = subjectPercentage,
                        recoveryClassesNeeded = recoveryClassesNeeded,
                        isAttendanceLow = subjectPercentage < reqFraction,
                        isTemporary = lecture.isTemporary,
                        temporaryLabel = lecture.temporaryLabel
                    )
                }

                val reqFraction = requirement / 100f
                val overallRecoveryClassesNeeded = if (percentage < reqFraction && reqFraction < 1f) {
                    val x = (reqFraction * overallTotal - globalPresentCount) / (1f - reqFraction)
                    if (x > 0) kotlin.math.ceil(x.toDouble()).toInt() else null
                } else null

                val overallClassesCanMiss = if (percentage >= reqFraction && reqFraction < 1f) {
                    val y = (globalPresentCount - reqFraction * overallTotal) / reqFraction
                    if (y > 0) kotlin.math.floor(y.toDouble()).toInt() else null
                } else null

                HomeUiState(
                    isLoading = false,
                    attendanceRequirement = requirement,
                    overallPresent = globalPresentCount,
                    overallAbsent = globalAbsentCount,
                    overallPercentage = percentage,
                    todayClasses = classUiModels,
                    hasTimetable = hasTimetable,
                    overallRecoveryClassesNeeded = overallRecoveryClassesNeeded,
                    overallClassesCanMiss = overallClassesCanMiss,
                    isHoliday = isHoliday
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun markAttendance(recordId: Long?, entryId: Long, subjectId: Long, startTime: Int, status: AttendanceStatus) {
        viewModelScope.launch {
            val todayStartEpoch = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            
            if (recordId != null) {
                // Exact target
                val record = attendanceRepository.getAllRecords().first().find { it.id == recordId }
                if (record != null) {
                    if (record.status == status) {
                        attendanceRepository.deleteRecord(record)
                    } else {
                        attendanceRepository.updateRecord(record.copy(status = status))
                    }
                }
            } else {
                // Creates exactly one occurrence as it does not exist
                attendanceRepository.upsertRecord(subjectId, todayStartEpoch, startTime, status)
            }
        }
    }

    fun markBulkAttendance(status: AttendanceStatus) {
        viewModelScope.launch {
            val validTodayClasses = _uiState.value.todayClasses.filter { 
                it.attendanceStatus != AttendanceStatus.CANCELLED 
            }
            if (validTodayClasses.isEmpty()) return@launch

            val todayStartEpoch = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val allRecords = attendanceRepository.getAllRecords().first()
            
            val previousStates = validTodayClasses.map { cls ->
                Pair(cls, if (cls.attendanceRecordId != null) allRecords.find { it.id == cls.attendanceRecordId } else null)
            }
            
            validTodayClasses.forEach { cls ->
                val prevRecord = previousStates.find { it.first.entryId == cls.entryId }?.second
                if (prevRecord != null && prevRecord.status != status) {
                    attendanceRepository.updateRecord(prevRecord.copy(status = status))
                } else if (prevRecord == null) {
                    attendanceRepository.upsertRecord(cls.subjectId, todayStartEpoch, cls.startTime, status)
                }
            }

            lastUndoAction = {
                val currentRecords = attendanceRepository.getAllRecords().first()
                previousStates.forEach { (cls, oldRecord) ->
                    if (oldRecord == null) {
                        val newlyInserted = currentRecords.find { it.subjectId == cls.subjectId && it.date == todayStartEpoch && it.startTime == cls.startTime }
                        if (newlyInserted != null) {
                            attendanceRepository.deleteRecord(newlyInserted)
                        }
                    } else {
                        attendanceRepository.updateRecord(oldRecord)
                    }
                }
            }
        }
    }

    fun markHoliday() {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            holidayRepository.markHoliday(today)
            lastUndoAction = {
                holidayRepository.deleteHoliday(today)
            }
        }
    }

    fun unmarkHoliday() {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            holidayRepository.deleteHoliday(today)
        }
    }

    fun performUndo() {
        viewModelScope.launch {
            lastUndoAction?.invoke()
            lastUndoAction = null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as PresentApplication)
                HomeViewModel(
                    attendanceRepository = application.container.attendanceRepository,
                    timetableRepository = application.container.timetableRepository,
                    userPreferencesRepository = application.container.userPreferencesRepository,
                    holidayRepository = application.container.holidayRepository,
                    temporaryLectureRepository = application.container.temporaryLectureRepository
                )
            }
        }
    }
}
