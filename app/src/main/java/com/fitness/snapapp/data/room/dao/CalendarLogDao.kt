package com.fitness.snapapp.data.room.dao

import androidx.room.*
import com.fitness.snapapp.data.room.entity.CalendarLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarLogDao {
    @Query("SELECT * FROM calendar_log ORDER BY date DESC")
    fun getAll(): Flow<List<CalendarLogEntity>>

    @Query("SELECT * FROM calendar_log WHERE date >= :fromEpoch")
    suspend fun getLogsFrom(fromEpoch: Long): List<CalendarLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: CalendarLogEntity)
}
