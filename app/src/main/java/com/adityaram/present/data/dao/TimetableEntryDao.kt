package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adityaram.present.data.model.TimetableEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableEntryDao {
    @Query("SELECT * FROM timetable_entries")
    fun getAllEntries(): Flow<List<TimetableEntry>>

    @Insert
    suspend fun insertAll(entries: List<TimetableEntry>)
    
    @androidx.room.Update
    suspend fun update(entry: TimetableEntry)
    @androidx.room.Delete
    suspend fun delete(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries WHERE subjectId = :subjectId")
    suspend fun deleteBySubjectId(subjectId: Long)
    
    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()
}
