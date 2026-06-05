package com.fitness.snapapp.ai.pose

/**
 * Immutable pose object — exists only in RAM during a single frame.
 * NEVER serialized, written to disk, or added to any collection.
 */
data class Pose(
    val keypoints: Array<Keypoint>,   // always size 17 — COCO keypoint order
    val timestampMs: Long = System.currentTimeMillis()
) {
    // Convenience accessors — COCO keypoint indices
    val nose:          Keypoint get() = keypoints[0]
    val leftEye:       Keypoint get() = keypoints[1]
    val rightEye:      Keypoint get() = keypoints[2]
    val leftEar:       Keypoint get() = keypoints[3]
    val rightEar:      Keypoint get() = keypoints[4]
    val leftShoulder:  Keypoint get() = keypoints[5]
    val rightShoulder: Keypoint get() = keypoints[6]
    val leftElbow:     Keypoint get() = keypoints[7]
    val rightElbow:    Keypoint get() = keypoints[8]
    val leftWrist:     Keypoint get() = keypoints[9]
    val rightWrist:    Keypoint get() = keypoints[10]
    val leftHip:       Keypoint get() = keypoints[11]
    val rightHip:      Keypoint get() = keypoints[12]
    val leftKnee:      Keypoint get() = keypoints[13]
    val rightKnee:     Keypoint get() = keypoints[14]
    val leftAnkle:     Keypoint get() = keypoints[15]
    val rightAnkle:    Keypoint get() = keypoints[16]

    // Disable equality — poses are ephemeral, no two should compare equal
    override fun equals(other: Any?) = false
    override fun hashCode() = System.identityHashCode(this)
}

data class Keypoint(val x: Float, val y: Float, val confidence: Float)

/**
 * Converts raw HRNet float[] output (heatmaps flattened as [K, H, W]) into a Pose.
 * HRNet output: 17 × 64 × 48  →  3072 values per keypoint  →  52224 total floats.
 *
 * Called on Thread 3 (Exercise Logic CPU thread).
 * The resulting Pose is valid for one frame only.
 */
object PoseExtractor {
    private const val HEATMAP_W = 48
    private const val HEATMAP_H = 64

    fun extractFromHeatmaps(heatmaps: FloatArray, imageW: Int, imageH: Int): Pose {
        require(heatmaps.size >= 17 * HEATMAP_W * HEATMAP_H) {
            "Heatmap array too small: expected ${17 * HEATMAP_W * HEATMAP_H}, got ${heatmaps.size}"
        }

        val keypoints = Array(17) { k ->
            val offset = k * HEATMAP_W * HEATMAP_H
            var maxVal = Float.MIN_VALUE
            var maxX = 0
            var maxY = 0
            for (y in 0 until HEATMAP_H) {
                for (x in 0 until HEATMAP_W) {
                    val v = heatmaps[offset + y * HEATMAP_W + x]
                    if (v > maxVal) {
                        maxVal = v; maxX = x; maxY = y
                    }
                }
            }
            // Map heatmap coordinates back to image pixel space
            Keypoint(
                x = maxX.toFloat() / HEATMAP_W * imageW,
                y = maxY.toFloat() / HEATMAP_H * imageH,
                confidence = maxVal
            )
        }
        return Pose(keypoints)
    }

    /**
     * Build a Pose directly from the flat FloatArray returned by SNPEHelper.runInference().
     * Layout per person (after the bounding box 4 floats): 17 × [x, y, conf]
     *
     * @param inferenceOutput  full output array from runInference()
     * @param personIndex      0-based index of the person to decode
     */
    fun fromInferenceOutput(inferenceOutput: FloatArray, personIndex: Int = 0): Pose? {
        if (inferenceOutput.isEmpty()) return null
        val numPersons = inferenceOutput[0].toInt()
        if (personIndex >= numPersons) return null

        // Layout: [numPersons, (x1,y1,x2,y2, kp0x,kp0y,kp0c, ..., kp16x,kp16y,kp16c)]
        val stride = 4 + 17 * 3   // 4 box coords + 51 keypoint floats
        val base   = 1 + personIndex * stride + 4  // skip numPersons token + box

        if (base + 17 * 3 > inferenceOutput.size) return null

        val keypoints = Array(17) { k ->
            val i = base + k * 3
            Keypoint(inferenceOutput[i], inferenceOutput[i + 1], inferenceOutput[i + 2])
        }
        return Pose(keypoints)
    }
}
