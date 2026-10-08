package com.adityaram.present.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_plans")
data class LeavePlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDateEpochDays: Long,
    val endDateEpochDays: Long,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
