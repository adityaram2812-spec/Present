package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adityaram.present.data.model.Holiday
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays")
    fun getAllHolidays(): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHoliday(holiday: Holiday)

    @Query("DELETE FROM holidays WHERE localDateEpochDays = :epochDays")
    suspend fun deleteHoliday(epochDays: Long)
}
