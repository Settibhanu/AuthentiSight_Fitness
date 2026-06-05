package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val target: Int,
    val progress: Int,
    val completed: Boolean
)
