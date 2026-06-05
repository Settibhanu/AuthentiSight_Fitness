package com.fitness.snapapp.ai.yolo

/**
 * Data class representing a detected person bounding box.
 * Coordinates are in the original camera frame pixel space.
 */
data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val confidence: Float
) {
    val width: Float  get() = x2 - x1
    val height: Float get() = y2 - y1
    val cx: Float get() = (x1 + x2) / 2f
    val cy: Float get() = (y1 + y2) / 2f
}

/**
 * Parses the raw FloatArray from SNPEHelper.runInference() into Detection objects.
 *
 * Output layout from JNI:
 *   [numPersons, box0_x1, box0_y1, box0_x2, box0_y2, kp0_x, kp0_y, kp0_c, ...(×17),
 *                box1_x1, ...]
 */
object YoloNasDetector {
    private const val PERSON_STRIDE = 4 + 17 * 3  // box + 17 keypoints × 3 floats

    fun parseDetections(output: FloatArray): List<Detection> {
        if (output.isEmpty()) return emptyList()
        val count = output[0].toInt()
        if (count <= 0) return emptyList()

        return (0 until count).mapNotNull { i ->
            val base = 1 + i * PERSON_STRIDE
            if (base + 4 > output.size) return@mapNotNull null
            Detection(
                x1 = output[base],
                y1 = output[base + 1],
                x2 = output[base + 2],
                y2 = output[base + 3],
                confidence = 1f  // confidence is already filtered in C++ (threshold 0.20)
            )
        }
    }
}
