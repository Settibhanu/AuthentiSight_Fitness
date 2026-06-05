package com.fitness.snapapp.data.repository

import com.fitness.snapapp.data.room.dao.WorkoutSessionDao
import com.fitness.snapapp.data.room.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Only this repository class interacts with [WorkoutSessionDao].
 * The AI pipeline NEVER calls DAO methods directly.
 */
class WorkoutRepository(private val dao: WorkoutSessionDao) {

    val allSessions: Flow<List<WorkoutSessionEntity>> = dao.getAllSessions()

    fun sessionsSince(fromEpoch: Long): Flow<List<WorkoutSessionEntity>> =
        dao.getSessionsSince(fromEpoch)

    suspend fun saveSession(session: WorkoutSessionEntity) = dao.insert(session)

    suspend fun deleteSession(session: WorkoutSessionEntity) = dao.delete(session)

    suspend fun totalRepsForExercise(exercise: String): Int =
        dao.totalRepsForExercise(exercise)
}
