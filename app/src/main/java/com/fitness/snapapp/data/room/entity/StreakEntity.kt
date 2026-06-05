package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Singleton row — always id = 1 */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int,
    val bestStreak: Int,
    /** Epoch ms of the last completed workout day */
    val lastWorkoutDate: Long
)
