package com.adityaram.present.domain.repository

import com.adityaram.present.data.dao.AttendanceRecordDao
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import com.adityaram.present.data.AppDatabase

class AttendanceRepository(
    private val database: AppDatabase,
    private val attendanceRecordDao: AttendanceRecordDao
) {
    fun getAllRecords(): Flow<List<AttendanceRecord>> = attendanceRecordDao.getAllRecords()

    suspend fun upsertRecord(subjectId: Long, date: Long, startTime: Int, status: AttendanceStatus) {
        val existing = attendanceRecordDao.getRecord(subjectId, date, startTime)
        if (existing != null) {
            if (existing.status != status) {
                attendanceRecordDao.update(existing.copy(status = status))
            }
        } else {
            attendanceRecordDao.insert(
                AttendanceRecord(
                    subjectId = subjectId,
                    date = date,
                    startTime = startTime,
                    status = status,
                    note = null
                )
            )
        }
    }

    suspend fun updateRecord(record: AttendanceRecord) {
        attendanceRecordDao.update(record)
    }

    suspend fun deleteRecord(record: AttendanceRecord) {
        attendanceRecordDao.delete(record)
    }

    suspend fun bulkProcessAttendance(inserts: List<AttendanceRecord>, updates: List<AttendanceRecord>) {
        database.withTransaction {
            if (updates.isNotEmpty()) {
                attendanceRecordDao.updateAll(updates)
            }
            if (inserts.isNotEmpty()) {
                attendanceRecordDao.insertAll(inserts)
            }
        }
    }

    /**
     * Calculates the attendance percentage given a list of records.
     * CANCELLED records are ignored.
     */
    fun calculateAttendanceRatio(presentCount: Int, totalEligible: Int): Float {
        if (totalEligible == 0) return 1f
        return presentCount.toFloat() / totalEligible
    }



    suspend fun cancelOccurrence(subjectId: Long, date: Long, startTime: Int) {
        val existing = attendanceRecordDao.getRecord(subjectId, date, startTime)
        val note = if (existing != null && existing.status != AttendanceStatus.CANCELLED) {
            "PREV_STATUS:${existing.status.name}"
        } else {
            null
        }
        
        if (existing != null) {
            attendanceRecordDao.update(existing.copy(status = AttendanceStatus.CANCELLED, note = note))
        } else {
            attendanceRecordDao.insert(
                AttendanceRecord(
                    subjectId = subjectId,
                    date = date,
                    startTime = startTime,
                    status = AttendanceStatus.CANCELLED,
                    note = null
                )
            )
        }
    }

    suspend fun restoreOccurrence(subjectId: Long, date: Long, startTime: Int) {
        val existing = attendanceRecordDao.getRecord(subjectId, date, startTime)
        if (existing != null && existing.status == AttendanceStatus.CANCELLED) {
            val previousStatusStr = existing.note?.removePrefix("PREV_STATUS:")
            if (previousStatusStr != null && previousStatusStr != existing.note) {
                try {
                    val prevStatus = AttendanceStatus.valueOf(previousStatusStr)
                    attendanceRecordDao.update(existing.copy(status = prevStatus, note = null))
                } catch (e: Exception) {
                    attendanceRecordDao.delete(existing)
                }
            } else {
                attendanceRecordDao.delete(existing)
            }
        }
    }
}
