package com.fitness.snapapp.data.repository

import com.fitness.snapapp.data.room.dao.ChallengeDao
import com.fitness.snapapp.data.room.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

class ChallengeRepository(private val dao: ChallengeDao) {
    val allChallenges: Flow<List<ChallengeEntity>> = dao.getAll()
    val activeChallenges: Flow<List<ChallengeEntity>> = dao.getActive()

    suspend fun addChallenge(challenge: ChallengeEntity) = dao.upsert(challenge)
    suspend fun updateProgress(id: Int, progress: Int) {
        // Retrieve current target to check completion
        dao.updateProgress(id, progress, false)  // completion checked in domain layer
    }
    suspend fun markCompleted(id: Int, target: Int) = dao.updateProgress(id, target, true)
    suspend fun delete(challenge: ChallengeEntity) = dao.delete(challenge)
}
