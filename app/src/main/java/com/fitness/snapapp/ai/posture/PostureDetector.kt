package com.fitness.snapapp.ai.posture

import com.fitness.snapapp.ai.pose.Keypoint
import com.fitness.snapapp.ai.pose.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Thread 3 — Posture Check (CPU)
 *
 * Evaluates the current frame's pose for common form errors.
 * Returns an actionable feedback message when a problem is detected.
 * No pose history is stored.
 */
class PostureDetector {

    data class PostureResult(
        val isCorrect: Boolean,
        val feedbackMessage: String = ""
    )

    fun checkPosture(pose: Pose, exerciseType: String): PostureResult {
        return when (exerciseType.uppercase()) {
            "SQUATS"        -> checkSquatPosture(pose)
            "PUSH_UPS"      -> checkPushUpPosture(pose)
            "PLANK"         -> checkPlankPosture(pose)
            "LUNGES"        -> checkLungePosture(pose)
            "SIT_UPS"       -> checkSitUpPosture(pose)
            "JUMPING_JACKS" -> PostureResult(true)    // minimal risk exercise
            "BURPEES"       -> checkBurpeePosture(pose)
            else            -> PostureResult(true)
        }
    }

    // ─── Squat ───────────────────────────────────────────────────────────────
    private fun checkSquatPosture(pose: Pose): PostureResult {
        // Back should stay relatively upright
        val backAngle = calculateAngle(
            pose.leftHip, pose.leftShoulder,
            Keypoint(pose.leftShoulder.x, 0f, 1f)
        )
        if (backAngle < 150f)
            return PostureResult(false, "Straighten your back")

        // Knee should not travel far past toes
        val kneeForwardDelta = pose.leftKnee.x - pose.leftAnkle.x
        if (kneeForwardDelta > 40f)
            return PostureResult(false, "Knees too far forward")

        // Hip depth check: between 90–110° is the sweet spot for a deep squat
        val hipAngle = calculateAngle(pose.leftKnee, pose.leftHip, pose.leftShoulder)
        if (hipAngle in 110f..160f)
            return PostureResult(false, "Lower your hips further")

        return PostureResult(true)
    }

    // ─── Push-up ─────────────────────────────────────────────────────────────
    private fun checkPushUpPosture(pose: Pose): PostureResult {
        // Core check: hips should not sag below the shoulder-ankle midline
        val midlineY = (pose.leftShoulder.y + pose.leftAnkle.y) / 2f
        val hipSag   = abs(pose.leftHip.y - midlineY)
        if (hipSag > 60f)
            return PostureResult(false, "Keep your core tight — hips dropping")

        // Neck should be neutral (not craning forward)
        val neckAngle = calculateAngle(
            pose.leftShoulder,
            Keypoint(pose.leftShoulder.x, pose.leftShoulder.y - 50f, 1f),
            pose.nose
        )
        if (neckAngle < 140f)
            return PostureResult(false, "Keep your neck neutral")

        return PostureResult(true)
    }

    // ─── Plank ───────────────────────────────────────────────────────────────
    private fun checkPlankPosture(pose: Pose): PostureResult {
        val bodyLineAngle = calculateAngle(pose.leftShoulder, pose.leftHip, pose.leftAnkle)
        if (bodyLineAngle < 160f)
            return PostureResult(false, "Keep your body in a straight line")
        return PostureResult(true)
    }

    // ─── Lunge ───────────────────────────────────────────────────────────────
    private fun checkLungePosture(pose: Pose): PostureResult {
        val kneeAngle = calculateAngle(pose.leftHip, pose.leftKnee, pose.leftAnkle)
        if (kneeAngle < 80f)
            return PostureResult(false, "Don't let your knee go past your toes")
        return PostureResult(true)
    }

    // ─── Sit-up ──────────────────────────────────────────────────────────────
    private fun checkSitUpPosture(pose: Pose): PostureResult {
        // Hands should stay near the head (not pulling on neck)
        val handToEarDist = abs(pose.leftWrist.x - pose.leftEar.x) +
                            abs(pose.leftWrist.y - pose.leftEar.y)
        if (handToEarDist < 30f)
            return PostureResult(false, "Don't pull on your neck")
        return PostureResult(true)
    }

    // ─── Burpee ──────────────────────────────────────────────────────────────
    private fun checkBurpeePosture(pose: Pose): PostureResult {
        // During plank phase of burpee — same as plank check
        val isPlankPhase = abs(pose.leftHip.y - pose.leftShoulder.y) < 50f
        if (isPlankPhase) return checkPlankPosture(pose)
        return PostureResult(true)
    }

    // ─── Angle utility ───────────────────────────────────────────────────────
    private fun calculateAngle(a: Keypoint, b: Keypoint, c: Keypoint): Float {
        val radians = atan2(c.y - b.y, c.x - b.x) - atan2(a.y - b.y, a.x - b.x)
        var angle = abs(radians * 180f / PI.toFloat())
        if (angle > 180f) angle = 360f - angle
        return angle
    }
}
