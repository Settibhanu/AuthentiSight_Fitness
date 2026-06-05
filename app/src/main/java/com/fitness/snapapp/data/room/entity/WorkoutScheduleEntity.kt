package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_schedule")
data class WorkoutScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** 1 = Monday … 7 = Sunday */
    val dayOfWeek: Int,
    val exercise: String,
    val targetReps: Int,
    val targetDurationSeconds: Int
)
