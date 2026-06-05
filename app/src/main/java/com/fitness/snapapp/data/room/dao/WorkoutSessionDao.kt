package com.fitness.snapapp.data.room.dao

import androidx.room.*
import com.fitness.snapapp.data.room.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE date >= :fromEpoch ORDER BY date DESC")
    fun getSessionsSince(fromEpoch: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT SUM(reps) FROM workout_session WHERE exercise = :exercise")
    suspend fun totalRepsForExercise(exercise: String): Int

    @Query("SELECT COUNT(*) FROM workout_session")
    suspend fun totalSessionCount(): Int

    @Delete
    suspend fun delete(session: WorkoutSessionEntity)
}
