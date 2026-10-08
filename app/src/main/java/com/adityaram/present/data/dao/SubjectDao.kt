package com.adityaram.present.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adityaram.present.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>
    
    @Insert
    suspend fun insert(subject: Subject): Long

    @androidx.room.Update
    suspend fun update(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteById(subjectId: Long)
}
