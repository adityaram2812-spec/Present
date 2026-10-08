package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.adityaram.present.data.model.LeavePlan
import kotlinx.coroutines.flow.Flow

@Dao
interface LeavePlanDao {
    @Query("SELECT * FROM leave_plans ORDER BY startDateEpochDays ASC")
    fun getAllLeavePlans(): Flow<List<LeavePlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leavePlan: LeavePlan)
    
    @Update
    suspend fun update(leavePlan: LeavePlan)

    @Delete
    suspend fun delete(leavePlan: LeavePlan)
    
    @Query("SELECT * FROM leave_plans WHERE id = :id")
    suspend fun getLeavePlan(id: Long): LeavePlan?
}
