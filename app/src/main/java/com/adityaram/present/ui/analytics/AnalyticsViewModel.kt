package com.adityaram.present.ui.analytics

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor

enum class AnalyticsRange {
    SEVEN_DAYS, THIRTY_DAYS, ALL_TIME
}

enum class SubjectStatus {
    SAFE, WARNING, CRITICAL
}

data class TrendBucket(
    val label: String,
    val presentCount: Int,
    val absentCount: Int
) {
    val total: Int get() = presentCount + absentCount
    val percentage: Float? get() = if (total > 0) presentCount.toFloat() / total.toFloat() else null
}

data class SubjectAnalyticsModel(
    val subject: Subject,
    val presentCount: Int,
    val absentCount: Int,
    val percentage: Float,
    val target: Float,
    val status: SubjectStatus,
    val contextMessage: String
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedRange: AnalyticsRange = AnalyticsRange.ALL_TIME,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val targetRequirement: Float = 0.75f,
    val trendBuckets: List<TrendBucket> = emptyList(),
    val trendIntervalLabel: String = "",
    val subjectModels: List<SubjectAnalyticsModel> = emptyList()
) {
    val totalCount: Int get() = presentCount + absentCount
    val overallPercentage: Float? get() = if (totalCount > 0) presentCount.toFloat() / totalCount.toFloat() else null
    val targetRequirementPercent: Int get() = (targetRequirement * 100).toInt()
}

class AnalyticsViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(AnalyticsRange.ALL_TIME)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedRange,
        attendanceRepository.getAllRecords(),
        timetableRepository.getAllSubjects(),
        userPreferencesRepository.attendanceRequirement,
        userPreferencesRepository.openingBalances
    ) { range, records, subjects, requirement, balancesJson ->
        val balances = parseOpeningBalances(balancesJson)
        val target = requirement / 100f
        val validRecords = records.filter { it.status != AttendanceStatus.CANCELLED }
        
        val today = LocalDate.now()
        val filterStartMillis: Long? = when (range) {
            AnalyticsRange.SEVEN_DAYS -> today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            AnalyticsRange.THIRTY_DAYS -> today.minusDays(29).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            AnalyticsRange.ALL_TIME -> null
        }

        val rangeRecords = if (filterStartMillis != null) {
            validRecords.filter { it.date >= filterStartMillis }
        } else {
            validRecords
        }

        var overallPresent = rangeRecords.count { it.status == AttendanceStatus.PRESENT }
        var overallAbsent = rangeRecords.count { it.status == AttendanceStatus.ABSENT }

        // Compile subject models
        val subjectModels = subjects.mapNotNull { subject ->
            val subjRecords = rangeRecords.filter { it.subjectId == subject.id }
            
            var p = subjRecords.count { it.status == AttendanceStatus.PRESENT }
            var a = subjRecords.count { it.status == AttendanceStatus.ABSENT }
            
            if (range == AnalyticsRange.ALL_TIME) {
                val ob = balances[subject.id]
                val obP = ob?.present ?: 0
                val obA = ob?.absent ?: 0
                p += obP
                a += obA
                
                // Add to overall if we haven't done it iteratively, wait, overallPresent must be calculated correctly.
                // It's better to add the totals below per-subject because overall metrics should sum all subjects' O.B.s.
            }
            // but wait, if it's all time, I also need to add to overall!
            if (p == 0 && a == 0) return@mapNotNull null

            val t = p + a
            val pct = p.toFloat() / t.toFloat()

            val status = when {
                pct < target -> SubjectStatus.CRITICAL
                pct < target + 0.05f -> SubjectStatus.WARNING // Less than 5% margin is warning
                else -> SubjectStatus.SAFE
            }

            val contextMessage = if (pct < target) {
                val needed = ceil((target * t - p) / (1 - target)).toInt()
                val deficit = ((target - pct) * 100)
                "Deficit ${String.format("%.1f", deficit)}% · Need $needed consecutive ${if (needed == 1) "present" else "presents"}"
            } else {
                val marginClasses = floor((p - target * t) / target).toInt()
                if (status == SubjectStatus.WARNING) {
                    val marginPct = ((pct - target) * 100)
                    "Margin ${String.format("%.1f", marginPct)}% · $marginClasses ${if (marginClasses == 1) "absence" else "absences"} to boundary"
                } else {
                    "$marginClasses ${if (marginClasses == 1) "class" else "classes"} of breathing room"
                }
            }

            SubjectAnalyticsModel(
                subject = subject,
                presentCount = p,
                absentCount = a,
                percentage = pct,
                target = target,
                status = status,
                contextMessage = contextMessage
            )
        }.sortedWith(compareBy<SubjectAnalyticsModel> { it.status.ordinal }.reversed().thenBy { it.subject.name }) // CRITICAL first

        if (range == AnalyticsRange.ALL_TIME) {
            overallPresent += balances.values.sumOf { it.present }
            overallAbsent += balances.values.sumOf { it.absent }
        }

        // Trend calculation
        val buckets = computeTrend(range, rangeRecords, today)
        val trendIntervalLabel = when (range) {
            AnalyticsRange.SEVEN_DAYS -> "Daily interval"
            AnalyticsRange.THIRTY_DAYS, AnalyticsRange.ALL_TIME -> "Weekly interval"
        }

        AnalyticsUiState(
            isLoading = false,
            selectedRange = range,
            presentCount = overallPresent,
            absentCount = overallAbsent,
            targetRequirement = target,
            trendBuckets = buckets,
            trendIntervalLabel = trendIntervalLabel,
            subjectModels = subjectModels
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState(isLoading = true))

    private fun computeTrend(range: AnalyticsRange, records: List<AttendanceRecord>, today: LocalDate): List<TrendBucket> {
        if (records.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        
        return when (range) {
            AnalyticsRange.SEVEN_DAYS -> {
                // 7 daily buckets ending on today
                (6 downTo 0).map { offset ->
                    val d = today.minusDays(offset.toLong())
                    val dStart = d.atStartOfDay(zone).toInstant().toEpochMilli()
                    val dEnd = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    
                    val bucketRecords = records.filter { it.date in dStart until dEnd }
                    val labelFormat = DateTimeFormatter.ofPattern("EEE")
                    TrendBucket(
                        label = d.format(labelFormat),
                        presentCount = bucketRecords.count { it.status == AttendanceStatus.PRESENT },
                        absentCount = bucketRecords.count { it.status == AttendanceStatus.ABSENT }
                    )
                }
            }
            AnalyticsRange.THIRTY_DAYS -> {
                // weekly buckets over 30 days
                val buckets = mutableListOf<TrendBucket>()
                for (w in 4 downTo 1) {
                    val endExcl = today.minusDays(((w - 1) * 7).toLong()).plusDays(1)
                    val startInc = endExcl.minusDays(7)
                    
                    val startMillis = startInc.atStartOfDay(zone).toInstant().toEpochMilli()
                    val endMillis = endExcl.atStartOfDay(zone).toInstant().toEpochMilli()
                    
                    val bucketRecords = records.filter { it.date in startMillis until endMillis }
                    buckets.add(
                        TrendBucket(
                            label = "W${5 - w}",
                            presentCount = bucketRecords.count { it.status == AttendanceStatus.PRESENT },
                            absentCount = bucketRecords.count { it.status == AttendanceStatus.ABSENT }
                        )
                    )
                }
                buckets
            }
            AnalyticsRange.ALL_TIME -> {
                // group by week backwards from today until we have at most 6 meaningful weeks
                // Find oldest record
                val oldestMillis = records.minOf { it.date }
                val oldestDate = Instant.ofEpochMilli(oldestMillis).atZone(zone).toLocalDate()
                val totalDays = ChronoUnit.DAYS.between(oldestDate, today)
                val totalWeeks = (totalDays / 7).toInt() + 1
                
                val maxWeeks = minOf(totalWeeks, 6)
                val buckets = mutableListOf<TrendBucket>()
                
                for (w in maxWeeks downTo 1) {
                    val endExcl = today.minusDays(((w - 1) * 7).toLong()).plusDays(1)
                    val startInc = endExcl.minusDays(7)
                    val startMillis = startInc.atStartOfDay(zone).toInstant().toEpochMilli()
                    val endMillis = endExcl.atStartOfDay(zone).toInstant().toEpochMilli()
                    
                    val bucketRecords = records.filter { it.date in startMillis until endMillis }
                    buckets.add(
                        TrendBucket(
                            label = "W${maxWeeks - w + 1}",
                            presentCount = bucketRecords.count { it.status == AttendanceStatus.PRESENT },
                            absentCount = bucketRecords.count { it.status == AttendanceStatus.ABSENT }
                        )
                    )
                }
                buckets
            }
        }
    }

    fun setRange(range: AnalyticsRange) {
        _selectedRange.value = range
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
                return AnalyticsViewModel(
                    attendanceRepository = appContainer.attendanceRepository,
                    timetableRepository = appContainer.timetableRepository,
                    userPreferencesRepository = appContainer.userPreferencesRepository
                ) as T
            }
        }
    }
}
