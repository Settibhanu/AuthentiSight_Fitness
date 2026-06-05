package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val heightCm: Float,
    val weightKg: Float,
    /** BEGINNER, INTERMEDIATE, or ADVANCED */
    val fitnessLevel: String
)
