package com.fitness.snapapp.domain.schedule

import com.fitness.snapapp.data.room.dao.WorkoutScheduleDao
import com.fitness.snapapp.data.room.entity.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Provides today's scheduled workouts and schedule CRUD.
 */
class ScheduleEngine(private val dao: WorkoutScheduleDao) {

    val fullSchedule: Flow<List<WorkoutScheduleEntity>> = dao.getAll()

    /** Returns the list of workouts scheduled for today (1=Mon … 7=Sun) */
    suspend fun getTodayWorkouts(): List<WorkoutScheduleEntity> {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Calendar: 1=Sun, 2=Mon … 7=Sat → convert to 1=Mon … 7=Sun
        val normalised = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        return dao.getForDay(normalised)
    }

    suspend fun addSchedule(schedule: WorkoutScheduleEntity) = dao.insert(schedule)
    suspend fun removeSchedule(schedule: WorkoutScheduleEntity) = dao.delete(schedule)
    suspend fun clearDay(dayOfWeek: Int) = dao.deleteForDay(dayOfWeek)
}
