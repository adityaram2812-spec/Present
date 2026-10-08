package com.adityaram.present.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey
    val localDateEpochDays: Long // Normalized local date (LocalDate.toEpochDay())
)
