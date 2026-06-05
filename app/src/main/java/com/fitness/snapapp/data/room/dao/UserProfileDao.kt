package com.fitness.snapapp.data.room.dao

import androidx.room.*
import com.fitness.snapapp.data.room.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)
}
