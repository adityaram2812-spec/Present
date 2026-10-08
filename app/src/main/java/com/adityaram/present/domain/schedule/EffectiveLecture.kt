package com.adityaram.present.domain.schedule

import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TimetableEntry

data class EffectiveLecture(
    val baseEntry: TimetableEntry?,
    val temporaryLecture: TemporaryLecture?,
    val subjectId: Long,
    val startTime: Int, // minutes from midnight
    val endTime: Int, // minutes from midnight
    val isTemporary: Boolean = false,
    val temporaryLabel: String? = null // e.g. "Replaces CM", "Extra lecture"
)
