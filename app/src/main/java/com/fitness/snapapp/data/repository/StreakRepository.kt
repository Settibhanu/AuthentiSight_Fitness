package com.fitness.snapapp.data.repository

import com.fitness.snapapp.data.room.dao.StreakDao
import com.fitness.snapapp.data.room.entity.StreakEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class StreakRepository(private val dao: StreakDao) {

    val streak: Flow<StreakEntity?> = dao.getStreak()

    /**
     * Update streak after a completed workout.
     * Increments currentStreak if the last workout was yesterday,
     * resets to 1 if a day was skipped, or keeps current if same day.
     */
    suspend fun recordWorkoutToday() {
        val now = System.currentTimeMillis()
        val todayMidnight = floorToDay(now)
        val existing = dao.getStreakOnce()

        val newStreak = if (existing == null) {
            StreakEntity(currentStreak = 1, bestStreak = 1, lastWorkoutDate = todayMidnight)
        } else {
            val lastDay = floorToDay(existing.lastWorkoutDate)
            val daysDiff = TimeUnit.MILLISECONDS.toDays(todayMidnight - lastDay)
            when {
                daysDiff == 0L -> existing  // same day, no change
                daysDiff == 1L -> {
                    val newCurrent = existing.currentStreak + 1
                    existing.copy(
                        currentStreak = newCurrent,
                        bestStreak = maxOf(existing.bestStreak, newCurrent),
                        lastWorkoutDate = todayMidnight
                    )
                }
                else -> existing.copy(
                    currentStreak = 1,
                    lastWorkoutDate = todayMidnight
                )
            }
        }
        dao.upsert(newStreak)
    }

    private fun floorToDay(epochMs: Long): Long {
        return (epochMs / TimeUnit.DAYS.toMillis(1)) * TimeUnit.DAYS.toMillis(1)
    }
}
