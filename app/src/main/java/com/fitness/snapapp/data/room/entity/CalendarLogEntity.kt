package com.fitness.snapapp.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_log")
data class CalendarLogEntity(
    /** Epoch ms for midnight UTC of the logged day (primary key — one row per day) */
    @PrimaryKey val date: Long,
    val completed: Boolean
)
