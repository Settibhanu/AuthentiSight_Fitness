package com.fitness.snapapp.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fitness.snapapp.data.room.dao.*
import com.fitness.snapapp.data.room.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        WorkoutSessionEntity::class,
        WorkoutScheduleEntity::class,
        CalendarLogEntity::class,
        StreakEntity::class,
        ChallengeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao():    UserProfileDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun calendarLogDao():    CalendarLogDao
    abstract fun streakDao():         StreakDao
    abstract fun challengeDao():      ChallengeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snapfitness.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
