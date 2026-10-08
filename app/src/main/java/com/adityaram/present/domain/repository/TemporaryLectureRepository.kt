package com.adityaram.present.domain.repository

import com.adityaram.present.data.dao.TemporaryLectureDao
import com.adityaram.present.data.model.TemporaryLecture
import kotlinx.coroutines.flow.Flow

class TemporaryLectureRepository(private val temporaryLectureDao: TemporaryLectureDao) {
    fun getAllTemporaryLectures(): Flow<List<TemporaryLecture>> = temporaryLectureDao.getAllTemporaryLectures()

    fun getTemporaryLecturesForDate(dateEpochDays: Long): Flow<List<TemporaryLecture>> = temporaryLectureDao.getTemporaryLecturesForDate(dateEpochDays)

    suspend fun getTemporaryLecture(id: Long): TemporaryLecture? = temporaryLectureDao.getTemporaryLectureById(id)

    suspend fun saveTemporaryLecture(lecture: TemporaryLecture): Long {
        return if (lecture.id == 0L) {
            temporaryLectureDao.insertTemporaryLecture(lecture)
        } else {
            temporaryLectureDao.updateTemporaryLecture(lecture)
            lecture.id
        }
    }

    suspend fun deleteTemporaryLecture(lecture: TemporaryLecture) {
        temporaryLectureDao.deleteTemporaryLecture(lecture)
    }
}
