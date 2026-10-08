package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adityaram.present.data.model.TemporaryLecture
import kotlinx.coroutines.flow.Flow

@Dao
interface TemporaryLectureDao {
    @Query("SELECT * FROM temporary_lectures ORDER BY dateEpochDays ASC, startTime ASC")
    fun getAllTemporaryLectures(): Flow<List<TemporaryLecture>>

    @Query("SELECT * FROM temporary_lectures WHERE id = :id LIMIT 1")
    suspend fun getTemporaryLectureById(id: Long): TemporaryLecture?

    @Query("SELECT * FROM temporary_lectures WHERE dateEpochDays = :dateEpochDays ORDER BY startTime ASC")
    fun getTemporaryLecturesForDate(dateEpochDays: Long): Flow<List<TemporaryLecture>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemporaryLecture(lecture: TemporaryLecture): Long

    @Update
    suspend fun updateTemporaryLecture(lecture: TemporaryLecture)

    @Delete
    suspend fun deleteTemporaryLecture(lecture: TemporaryLecture)
    
    @Query("DELETE FROM temporary_lectures WHERE dateEpochDays < :dateEpochDays")
    suspend fun clearOldTemporaryLectures(dateEpochDays: Long)
}
