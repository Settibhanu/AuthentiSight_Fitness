package com.fitness.snapapp.ai.counter

import com.fitness.snapapp.ai.pose.Keypoint
import com.fitness.snapapp.ai.pose.Pose
import com.fitness.snapapp.core.constants.AppConstants
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Thread 3 — Exercise Logic (CPU)
 *
 * Stateless rep counting via a two-phase (UP/DOWN) joint-angle state machine.
 * State is reset between exercises. No pose history is accumulated.
 * [process] is designed to be called once per camera frame.
 */
class ExerciseCounter(private val exerciseType: ExerciseType) {

    enum class ExerciseType {
        PUSH_UPS, SQUATS, LUNGES, PLANK, JUMPING_JACKS, SIT_UPS, BURPEES;

        companion object {
            fun fromString(s: String): ExerciseType =
                values().firstOrNull { it.name.equals(s, ignoreCase = true) } ?: SQUATS
        }
    }

    private enum class Phase { UP, DOWN }

    private var phase = Phase.UP

    var repCount: Int = 0
        private set

    /** Reset counter and phase — call at start of each new workout set. */
    fun reset() {
        phase = Phase.UP
        repCount = 0
    }

    /**
     * Process ONE frame's pose.
     * @return current rep count after processing this frame.
     * The Pose object is NOT stored after this call.
     */
    fun process(pose: Pose): Int {
        when (exerciseType) {
            ExerciseType.SQUATS        -> processSquat(pose)
            ExerciseType.PUSH_UPS      -> processPushUp(pose)
            ExerciseType.SIT_UPS       -> processSitUp(pose)
            ExerciseType.JUMPING_JACKS -> processJumpingJack(pose)
            ExerciseType.LUNGES        -> processLunge(pose)
            ExerciseType.BURPEES       -> processBurpee(pose)
            ExerciseType.PLANK         -> { /* time-based — no rep counting */ }
        }
        return repCount
    }

    // ─── Squat ───────────────────────────────────────────────────────────────
    // Hip angle > 160° (standing) → < 90° (deep squat) → > 160° = 1 rep
    private fun processSquat(pose: Pose) {
        val hipAngle = calculateAngle(pose.leftKnee, pose.leftHip, pose.leftShoulder)
        when (phase) {
            Phase.UP   -> if (hipAngle < AppConstants.SQUAT_DOWN_HIP_ANGLE) phase = Phase.DOWN
            Phase.DOWN -> if (hipAngle > AppConstants.SQUAT_UP_HIP_ANGLE)   { phase = Phase.UP; repCount++ }
        }
    }

    // ─── Push-up ─────────────────────────────────────────────────────────────
    // Elbow angle < 90° (chest down) → > 160° (arms extended) = 1 rep
    private fun processPushUp(pose: Pose) {
        val elbowAngle = calculateAngle(pose.leftShoulder, pose.leftElbow, pose.leftWrist)
        when (phase) {
            Phase.UP   -> if (elbowAngle < AppConstants.PUSHUP_ELBOW_DOWN_ANGLE) phase = Phase.DOWN
            Phase.DOWN -> if (elbowAngle > AppConstants.PUSHUP_ELBOW_UP_ANGLE)   { phase = Phase.UP; repCount++ }
        }
    }

    // ─── Sit-up ──────────────────────────────────────────────────────────────
    // Torso angle < 60° (curled up) → > 110° (lying flat) = 1 rep
    private fun processSitUp(pose: Pose) {
        val torsoAngle = calculateAngle(pose.leftHip, pose.leftShoulder, pose.nose)
        when (phase) {
            Phase.UP   -> if (torsoAngle < 60f)  phase = Phase.DOWN
            Phase.DOWN -> if (torsoAngle > 110f) { phase = Phase.UP; repCount++ }
        }
    }

    // ─── Jumping Jack ────────────────────────────────────────────────────────
    // Arm angle < 40° (arms down) → > 130° (arms overhead) = 1 rep
    private fun processJumpingJack(pose: Pose) {
        val armAngle = calculateAngle(pose.leftHip, pose.leftShoulder, pose.leftWrist)
        when (phase) {
            Phase.UP   -> if (armAngle < 40f)  phase = Phase.DOWN
            Phase.DOWN -> if (armAngle > 130f) { phase = Phase.UP; repCount++ }
        }
    }

    // ─── Lunge ───────────────────────────────────────────────────────────────
    // Knee angle < 100° (deep lunge) → > 150° (standing) = 1 rep
    private fun processLunge(pose: Pose) {
        val kneeAngle = calculateAngle(pose.leftHip, pose.leftKnee, pose.leftAnkle)
        when (phase) {
            Phase.UP   -> if (kneeAngle < 100f) phase = Phase.DOWN
            Phase.DOWN -> if (kneeAngle > 150f) { phase = Phase.UP; repCount++ }
        }
    }

    // ─── Burpee ──────────────────────────────────────────────────────────────
    // Detect standing → plank position → standing = 1 rep
    private fun processBurpee(pose: Pose) {
        val hipHeight      = pose.leftHip.y
        val shoulderHeight = pose.leftShoulder.y
        val isPlankPosition = abs(hipHeight - shoulderHeight) < 50f
        when (phase) {
            Phase.UP   -> if (isPlankPosition) phase = Phase.DOWN
            Phase.DOWN -> if (!isPlankPosition && hipHeight < shoulderHeight - 100f) {
                phase = Phase.UP; repCount++
            }
        }
    }

    // ─── Angle utility ───────────────────────────────────────────────────────
    /** Returns the angle at joint [b] formed by [a]–[b]–[c] in degrees (0–180). */
    private fun calculateAngle(a: Keypoint, b: Keypoint, c: Keypoint): Float {
        val radians = atan2(c.y - b.y, c.x - b.x) - atan2(a.y - b.y, a.x - b.x)
        var angle = abs(radians * 180f / PI.toFloat())
        if (angle > 180f) angle = 360f - angle
        return angle
    }
}
