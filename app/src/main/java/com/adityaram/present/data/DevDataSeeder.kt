package com.adityaram.present.data

import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * DEVELOPMENT ONLY SEEDER
 * Isolated script to populate the local Room database with realistic testing data.
 * This file is purely for QA matching against the Stitch design reference.
 */
object DevDataSeeder {
    
    // Toggle to TRUE to seed realistic sample data on startup (if database is empty).
    // MUST BE FALSE IN PRODUCTION.
    private const val ENABLE_SAMPLE_DATA = false
    
    // Toggle to TRUE to wipe all local data when the app launches (Useful to clear sample data).
    private const val WIPE_DATABASE_ON_STARTUP = false
    
    fun initializeDevEnvironment(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            if (WIPE_DATABASE_ON_STARTUP) {
                database.clearAllTables()
            }
            
            // Absolutely no sample data should be automatically injected.
            // All sample tracking has been hard-erased to prevent production leakage.
        }
    }
}
