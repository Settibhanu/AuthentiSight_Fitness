package com.fitness.snapapp.data.room.dao

import androidx.room.*
import com.fitness.snapapp.data.room.entity.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutScheduleDao {
    @Query("SELECT * FROM workout_schedule ORDER BY dayOfWeek ASC")
    fun getAll(): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedule WHERE dayOfWeek = :day")
    suspend fun getForDay(day: Int): List<WorkoutScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: WorkoutScheduleEntity)

    @Delete
    suspend fun delete(schedule: WorkoutScheduleEntity)

    @Query("DELETE FROM workout_schedule WHERE dayOfWeek = :day")
    suspend fun deleteForDay(day: Int)
}
