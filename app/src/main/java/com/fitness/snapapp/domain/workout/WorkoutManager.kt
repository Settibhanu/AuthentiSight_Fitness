package com.fitness.snapapp.domain.workout

import com.fitness.snapapp.ai.counter.ExerciseCounter
import com.fitness.snapapp.ai.pose.Pose
import com.fitness.snapapp.ai.posture.PostureDetector
import com.fitness.snapapp.core.utils.CalorieCalculator
import com.fitness.snapapp.data.repository.WorkoutRepository
import com.fitness.snapapp.data.room.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Domain coordinator for a single workout session.
 *
 * Frame processing:
 *   Each call to [processPose] accepts ONE pose, uses it, and drops it.
 *   The Pose is not stored in any collection.
 *
 * Posture tracking:
 *   Alert count / total count ratio → postureScore at workout end.
 */
class WorkoutManager(
    private val workoutRepository: WorkoutRepository,
    private val userWeightKg: Float = 70f
) {
    private var exerciseType: ExerciseCounter.ExerciseType = ExerciseCounter.ExerciseType.SQUATS
    private var counter       = ExerciseCounter(exerciseType)
    private val postureDetector = PostureDetector()

    // ── Observable state for UI ────────────────────────────────────────────
    private val _repCount    = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _postureAlert = MutableStateFlow<String?>(null)
    val postureAlert: StateFlow<String?> = _postureAlert.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // ── Posture score tracking (no pose objects stored) ────────────────────
    private var totalFrames   = 0
    private var correctFrames = 0

    val currentExerciseName: String get() = exerciseType.name

    // ── Session control ────────────────────────────────────────────────────

    fun startWorkout(type: ExerciseCounter.ExerciseType) {
        exerciseType   = type
        counter        = ExerciseCounter(type)
        totalFrames    = 0
        correctFrames  = 0
        _repCount.value     = 0
        _postureAlert.value = null
        _isActive.value     = true
    }

    fun pauseWorkout()  { _isActive.value = false }
    fun resumeWorkout() { _isActive.value = true  }

    /**
     * Called from the inference pipeline — once per camera frame.
     * Pose is consumed here and NEVER stored.
     */
    fun processPose(pose: Pose) {
        if (!_isActive.value) return

        _repCount.value = counter.process(pose)

        val result = postureDetector.checkPosture(pose, exerciseType.name)
        _postureAlert.value = if (!result.isCorrect) result.feedbackMessage else null

        totalFrames++
        if (result.isCorrect) correctFrames++
    }

    /**
     * Persist workout summary to Room. Resets all counters.
     * @param durationSeconds Elapsed time since [startWorkout]
     */
    suspend fun finishWorkout(durationSeconds: Int) {
        val postureScore  = if (totalFrames > 0) correctFrames.toFloat() / totalFrames else 0f
        val calories      = CalorieCalculator.estimate(exerciseType.name, userWeightKg, durationSeconds)

        val session = WorkoutSessionEntity(
            date            = System.currentTimeMillis(),
            exercise        = exerciseType.name,
            reps            = counter.repCount,
            durationSeconds = durationSeconds,
            calories        = calories,
            postureScore    = postureScore
        )
        workoutRepository.saveSession(session)

        // Reset
        _isActive.value     = false
        _repCount.value     = 0
        _postureAlert.value = null
        totalFrames         = 0
        correctFrames       = 0
        counter.reset()
    }
}
