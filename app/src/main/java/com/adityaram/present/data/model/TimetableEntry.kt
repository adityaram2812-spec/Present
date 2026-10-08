package com.adityaram.present.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1 (Mon) - 7 (Sun)
    val startTime: Int, // minutes from midnight
    val endTime: Int, // minutes from midnight
    val room: String?,
    val teacher: String?,
    val isRecurring: Boolean,
    val specificDateMillis: Long? = null
)
