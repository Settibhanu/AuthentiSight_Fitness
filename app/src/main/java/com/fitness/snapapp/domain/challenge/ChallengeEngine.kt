package com.fitness.snapapp.domain.challenge

import com.fitness.snapapp.data.repository.ChallengeRepository
import com.fitness.snapapp.data.room.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Manages challenge creation, progress updates, and completion detection.
 */
class ChallengeEngine(private val repo: ChallengeRepository) {

    val challenges: Flow<List<ChallengeEntity>> = repo.allChallenges
    val activeChallenges: Flow<List<ChallengeEntity>> = repo.activeChallenges

    /** Default challenges seeded on first launch */
    val defaultChallenges = listOf(
        ChallengeEntity(title = "100 Squats in a Week",  target = 100, progress = 0, completed = false),
        ChallengeEntity(title = "30-Day Push-Up Streak", target = 30,  progress = 0, completed = false),
        ChallengeEntity(title = "500 Total Reps",        target = 500, progress = 0, completed = false),
        ChallengeEntity(title = "Plank for 5 Minutes",   target = 300, progress = 0, completed = false)
    )

    suspend fun seedDefaults() = defaultChallenges.forEach { repo.addChallenge(it) }

    /**
     * Record progress after a workout. Auto-marks as completed when target reached.
     */
    suspend fun recordReps(challengeId: Int, currentProgress: Int, target: Int) {
        if (currentProgress >= target) {
            repo.markCompleted(challengeId, target)
        } else {
            repo.updateProgress(challengeId, currentProgress)
        }
    }

    suspend fun addChallenge(title: String, target: Int) =
        repo.addChallenge(ChallengeEntity(title = title, target = target, progress = 0, completed = false))

    suspend fun delete(challenge: ChallengeEntity) = repo.delete(challenge)
}
