package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores only post-workout summaries — never raw frames or pose data.
 */
@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Epoch milliseconds — workout start time */
    val date: Long,
    val exercise: String,
    val reps: Int,
    val durationSeconds: Int,
    val calories: Float,
    /** 0.0–1.0: fraction of frames with correct posture */
    val postureScore: Float
)
