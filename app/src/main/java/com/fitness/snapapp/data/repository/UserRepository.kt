package com.fitness.snapapp.data.repository

import com.fitness.snapapp.data.room.dao.UserProfileDao
import com.fitness.snapapp.data.room.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(private val dao: UserProfileDao) {
    val profile: Flow<UserProfileEntity?> = dao.getProfile()
    suspend fun saveProfile(profile: UserProfileEntity) = dao.upsert(profile)
}
