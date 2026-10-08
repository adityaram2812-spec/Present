package com.adityaram.present.domain.repository

import com.adityaram.present.data.AppDatabase
import com.adityaram.present.data.dao.AttendanceRecordDao
import com.adityaram.present.data.dao.SubjectDao
import com.adityaram.present.data.dao.TimetableEntryDao
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TimetableEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

class TimetableRepository(
    private val database: AppDatabase,
    private val subjectDao: SubjectDao,
    private val timetableEntryDao: TimetableEntryDao,
    private val attendanceRecordDao: AttendanceRecordDao
) {
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()
    fun getAllEntries(): Flow<List<TimetableEntry>> = timetableEntryDao.getAllEntries()

    fun hasTimetableEntries(): Flow<Boolean> = timetableEntryDao.getAllEntries().map { it.isNotEmpty() }

    suspend fun insertSubject(subject: Subject): Long = subjectDao.insert(subject)
    
    suspend fun insertEntries(entries: List<TimetableEntry>) {
        timetableEntryDao.insertAll(entries)
    }

    suspend fun updateEntry(entry: TimetableEntry) {
        timetableEntryDao.update(entry)
    }

    suspend fun deleteEntry(entry: TimetableEntry) {
        timetableEntryDao.delete(entry)
    }

    suspend fun updateSubject(subject: Subject) {
        subjectDao.update(subject)
    }

    suspend fun deleteSubjectSafely(subjectId: Long) {
        database.withTransaction {
            attendanceRecordDao.deleteBySubjectId(subjectId)
            timetableEntryDao.deleteBySubjectId(subjectId)
            subjectDao.deleteById(subjectId)
        }
    }

    suspend fun replaceTimetable(insertNewEntriesBlock: suspend () -> Unit) {
        database.withTransaction {
            timetableEntryDao.deleteAllEntries()
            insertNewEntriesBlock()
        }
    }
}
