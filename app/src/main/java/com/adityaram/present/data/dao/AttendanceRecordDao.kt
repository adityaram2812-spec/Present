package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adityaram.present.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

import androidx.room.Update

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records")
    fun getAllRecords(): Flow<List<AttendanceRecord>>
    
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND startTime = :startTime LIMIT 1")
    suspend fun getRecord(subjectId: Long, date: Long, startTime: Int): AttendanceRecord?

    @Insert
    suspend fun insert(record: AttendanceRecord)
    
    @Insert
    suspend fun insertAll(records: List<AttendanceRecord>)

    @Update
    suspend fun update(record: AttendanceRecord)
    
    @Update
    suspend fun updateAll(records: List<AttendanceRecord>)
    
    @androidx.room.Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteBySubjectId(subjectId: Long)
}
