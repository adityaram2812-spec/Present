package com.adityaram.present

import android.app.Application
import android.content.Context
import com.adityaram.present.data.AppDatabase
import com.adityaram.present.data.UserPreferencesRepository
import com.adityaram.present.data.dataStore
import com.adityaram.present.domain.repository.AttendanceRepository
import com.adityaram.present.domain.repository.TimetableRepository

interface AppContainer {
    val userPreferencesRepository: UserPreferencesRepository
    val attendanceRepository: AttendanceRepository
    val timetableRepository: TimetableRepository
    val authRepository: com.adityaram.present.data.auth.AuthRepository
    val cloudUserRepository: com.adityaram.present.data.account.CloudUserRepository
    val holidayRepository: com.adityaram.present.domain.repository.HolidayRepository
    val leavePlanRepository: com.adityaram.present.domain.repository.LeavePlanRepository
    val temporaryLectureRepository: com.adityaram.present.domain.repository.TemporaryLectureRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }
    
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    
    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepository(
            database = database,
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }
    
    override val timetableRepository: TimetableRepository by lazy {
        TimetableRepository(
            database = database,
            subjectDao = database.subjectDao(), 
            timetableEntryDao = database.timetableEntryDao(),
            attendanceRecordDao = database.attendanceRecordDao()
        )
    }
    
    override val authRepository: com.adityaram.present.data.auth.AuthRepository by lazy {
        com.adityaram.present.data.auth.AuthRepository(com.google.firebase.auth.FirebaseAuth.getInstance())
    }
    
    override val cloudUserRepository: com.adityaram.present.data.account.CloudUserRepository by lazy {
        com.adityaram.present.data.account.CloudUserRepository(
            com.google.firebase.auth.FirebaseAuth.getInstance(),
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        )
    }
    
    override val holidayRepository: com.adityaram.present.domain.repository.HolidayRepository by lazy {
        com.adityaram.present.domain.repository.HolidayRepository(database.holidayDao())
    }
    
    override val leavePlanRepository: com.adityaram.present.domain.repository.LeavePlanRepository by lazy {
        com.adityaram.present.domain.repository.LeavePlanRepository(database.leavePlanDao())
    }
    
    override val temporaryLectureRepository: com.adityaram.present.domain.repository.TemporaryLectureRepository by lazy {
        com.adityaram.present.domain.repository.TemporaryLectureRepository(database.temporaryLectureDao())
    }
}

class PresentApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        if (com.adityaram.present.BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        
        container = DefaultAppContainer(this)
        
        // Dev hook: Initialize the seeder if enabled. Completely isolated from normal repositories.
        com.adityaram.present.data.DevDataSeeder.initializeDevEnvironment(
            com.adityaram.present.data.AppDatabase.getDatabase(this)
        )
    }
}
