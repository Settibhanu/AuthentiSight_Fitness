package com.fitness.snapapp.core.utils

/**
 * Simple MET-based calorie estimator.
 * Used at workout end — never called per-frame.
 */
object CalorieCalculator {

    // MET (metabolic equivalent) values per exercise
    private val MET = mapOf(
        "SQUATS"        to 5.0,
        "PUSH_UPS"      to 8.0,
        "SIT_UPS"       to 6.0,
        "LUNGES"        to 6.0,
        "JUMPING_JACKS" to 7.7,
        "BURPEES"       to 10.0,
        "PLANK"         to 4.0
    )

    /**
     * Estimate calories burned.
     * @param exercise  ExerciseType name (e.g. "SQUATS")
     * @param weightKg  User body weight
     * @param durationSeconds Workout duration
     */
    fun estimate(exercise: String, weightKg: Float, durationSeconds: Int): Float {
        val met = MET[exercise.uppercase()] ?: 5.0
        val hours = durationSeconds / 3600.0
        return (met * weightKg * hours).toFloat()
    }
}
