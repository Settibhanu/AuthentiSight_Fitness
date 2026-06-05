package com.fitness.snapapp.data.room.dao

import androidx.room.*
import com.fitness.snapapp.data.room.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenge ORDER BY completed ASC, id DESC")
    fun getAll(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenge WHERE completed = 0")
    fun getActive(): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(challenge: ChallengeEntity)

    @Delete
    suspend fun delete(challenge: ChallengeEntity)

    @Query("UPDATE challenge SET progress = :progress, completed = :completed WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: Int, completed: Boolean)
}
