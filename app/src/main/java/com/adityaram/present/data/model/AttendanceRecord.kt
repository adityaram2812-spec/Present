package com.adityaram.present.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED
}

class Converters {
    @TypeConverter
    fun toAttendanceStatus(value: String) = enumValueOf<AttendanceStatus>(value)

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name
}

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val date: Long,
    val startTime: Int, // minutes from midnight to match timetable
    val status: AttendanceStatus,
    val note: String?
)
