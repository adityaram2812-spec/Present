package com.adityaram.present.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TemporaryLectureType {
    ADDITION,
    REPLACEMENT
}

class TemporaryLectureConverters {
    @TypeConverter
    fun toTemporaryLectureType(value: String) = enumValueOf<TemporaryLectureType>(value)

    @TypeConverter
    fun fromTemporaryLectureType(value: TemporaryLectureType) = value.name
}

@Entity(tableName = "temporary_lectures")
data class TemporaryLecture(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochDays: Long,
    val startTime: Int, // minutes from midnight
    val endTime: Int, // minutes from midnight
    val subjectId: Long,
    val teacher: String?,
    val room: String?,
    val type: TemporaryLectureType,
    val replacesEntryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
