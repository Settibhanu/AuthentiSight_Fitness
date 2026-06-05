package com.fitness.snapapp.domain.streak

import com.fitness.snapapp.data.repository.StreakRepository
import com.fitness.snapapp.data.room.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

/**
 * Wraps [StreakRepository] with a domain-friendly API.
 */
class StreakEngine(private val repo: StreakRepository) {

    val streak: Flow<StreakEntity?> = repo.streak

    /** Call once at the end of each completed workout day. */
    suspend fun onWorkoutCompleted() = repo.recordWorkoutToday()
}
