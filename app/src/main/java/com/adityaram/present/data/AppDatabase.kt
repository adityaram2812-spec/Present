package com.adityaram.present.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.adityaram.present.data.dao.AttendanceRecordDao
import com.adityaram.present.data.dao.SubjectDao
import com.adityaram.present.data.dao.TimetableEntryDao
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.data.dao.HolidayDao
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.Converters
import com.adityaram.present.data.model.Holiday
import com.adityaram.present.data.model.LeavePlan
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.dao.TemporaryLectureDao
import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TemporaryLectureConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Note: Strict non-destructive migration pathway enabled.
@Database(entities = [Subject::class, TimetableEntry::class, AttendanceRecord::class, Holiday::class, LeavePlan::class, TemporaryLecture::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class, TemporaryLectureConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableEntryDao(): TimetableEntryDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun holidayDao(): HolidayDao
    abstract fun leavePlanDao(): com.adityaram.present.data.dao.LeavePlanDao
    abstract fun temporaryLectureDao(): TemporaryLectureDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `holidays` (`localDateEpochDays` INTEGER NOT NULL, PRIMARY KEY(`localDateEpochDays`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `leave_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startDateEpochDays` INTEGER NOT NULL, `endDateEpochDays` INTEGER NOT NULL, `reason` TEXT, `createdAt` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `temporary_lectures` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateEpochDays` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `subjectId` INTEGER NOT NULL, `teacher` TEXT, `room` TEXT, `type` TEXT NOT NULL, `replacesEntryId` INTEGER, `createdAt` INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "present_db")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
