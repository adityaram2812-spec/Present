package com.adityaram.present.domain.repository

import com.adityaram.present.data.dao.HolidayDao
import com.adityaram.present.data.model.Holiday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HolidayRepository(private val holidayDao: HolidayDao) {
    fun getAllHolidays(): Flow<List<Holiday>> = holidayDao.getAllHolidays()

    suspend fun markHoliday(date: LocalDate) {
        holidayDao.insertHoliday(Holiday(date.toEpochDay()))
    }

    suspend fun deleteHoliday(date: LocalDate) {
        holidayDao.deleteHoliday(date.toEpochDay())
    }
}
