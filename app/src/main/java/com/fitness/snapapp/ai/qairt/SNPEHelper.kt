package com.fitness.snapapp.ai.qairt

import android.content.Context
import android.util.Log
import com.fitness.snapapp.BuildConfig
import kotlin.math.sin

/**
 * Kotlin-side SNPE entry point.
 *
 * Two modes:
 *   NATIVE  — real SNPE inference via libsnpe_jni.so (requires resolveDependencies.sh + DLC files)
 *   MOCK    — synthetic pose data for UI / logic testing without any models or native library
 *
 * The mode is selected automatically:
 *   - If BuildConfig.NATIVE_INFERENCE_ENABLED is true AND the native library loads → NATIVE
 *   - Otherwise → MOCK (app stays fully functional for UI testing)
 *
 * Runtime options (NATIVE mode only):
 *   'C' = CPU  (Oryon)
 *   'G' = GPU  (Adreno)
 *   'D' = DSP/HTP (Hexagon NPU V79) — production default
 */
class SNPEHelper(private val context: Context) {

    companion object {
        private const val TAG = "SNPEHelper"

        private var nativeLibLoaded = false

        init {
            if (BuildConfig.NATIVE_INFERENCE_ENABLED) {
                nativeLibLoaded = try {
                    System.loadLibrary("snpe_jni")
                    true
                } catch (e: UnsatisfiedLinkError) {
                    Log.w(TAG, "libsnpe_jni.so not found — running in MOCK mode. ${e.message}")
                    false
                }
            } else {
                Log.i(TAG, "Native inference disabled at build time — running in MOCK mode.")
            }
        }
    }

    private val useMock: Boolean get() = !nativeLibLoaded

    private var initialized = false

    // Mock pose generation state
    private var mockFrameIndex = 0L

    /**
     * Initialize both YOLO-NAS and HRNet networks.
     * In MOCK mode this always returns true immediately.
     */
    fun initialize(runtime: Char = 'D'): Boolean {
        if (useMock) {
            Log.i(TAG, "MOCK mode — skipping DLC load")
            initialized = true
            return true
        }

        val yoloDlc  = loadAsset("Quant_yoloNas_s_320.dlc")
        val hrnetDlc = loadAsset("hrnet_axis_int8.dlc")

        if (yoloDlc == null || hrnetDlc == null) {
            Log.w(TAG, "DLC assets missing — falling back to MOCK mode")
            initialized = true   // still mark ready so UI works
            return true
        }

        val bbOk   = buildNetworkBB(yoloDlc,   yoloDlc.size,   runtime)
        val poseOk = buildNetworkPose(hrnetDlc, hrnetDlc.size,  runtime)
        initialized = bbOk && poseOk
        Log.i(TAG, "SNPE init: detector=$bbOk pose=$poseOk runtime=$runtime → ok=$initialized")
        return initialized
    }

    fun isReady(): Boolean = initialized
    fun isMockMode(): Boolean = useMock

    /**
     * Run inference. In MOCK mode returns synthetic animated pose data
     * so the exercise counter and posture detector can be tested end-to-end.
     *
     * Output layout (both modes):
     *   [numPersons,
     *    box0_x1, box0_y1, box0_x2, box0_y2,
     *    kp0_x, kp0_y, kp0_conf, ... (×17)]
     */
    fun runInference(rgbaBytes: ByteArray, width: Int, height: Int): FloatArray {
        if (!initialized) return FloatArray(0)

        return if (useMock) {
            generateMockPose(width, height)
        } else {
            runInferenceNative(rgbaBytes, width, height)
        }
    }

    fun destroy() {
        if (!useMock && nativeLibLoaded) destroyNative()
        initialized = false
    }

    // ── Mock pose generator ───────────────────────────────────────────────────

    /**
     * Generates one synthetic person doing a continuous squat animation.
     * The hip angle oscillates between 100° (down) and 170° (up) so the
     * ExerciseCounter will count real reps even without a camera or models.
     */
    private fun generateMockPose(imageW: Int, imageH: Int): FloatArray {
        mockFrameIndex++
        val t = mockFrameIndex * 0.05   // animation speed

        val cx = imageW  * 0.5f
        val cy = imageH  * 0.5f

        // Squat cycle: hip Y moves up and down
        val squatPhase = sin(t).toFloat()   // -1..+1

        // Body proportions relative to frame size
        val scale = imageH * 0.35f

        // Approximate 17 COCO keypoints for a standing/squatting figure
        // Index: 0=nose 1=leftEye 2=rightEye 3=leftEar 4=rightEar
        //        5=leftShoulder 6=rightShoulder 7=leftElbow 8=rightElbow
        //        9=leftWrist 10=rightWrist 11=leftHip 12=rightHip
        //        13=leftKnee 14=rightKnee 15=leftAnkle 16=rightAnkle

        val hipDropY = squatPhase * scale * 0.25f  // hips drop on squat

        val kps = floatArrayOf(
            // x,                   y,                            conf
            cx,                     cy - scale * 0.90f,           0.95f,  // 0 nose
            cx - scale * 0.06f,     cy - scale * 0.95f,           0.90f,  // 1 leftEye
            cx + scale * 0.06f,     cy - scale * 0.95f,           0.90f,  // 2 rightEye
            cx - scale * 0.10f,     cy - scale * 0.88f,           0.85f,  // 3 leftEar
            cx + scale * 0.10f,     cy - scale * 0.88f,           0.85f,  // 4 rightEar
            cx - scale * 0.20f,     cy - scale * 0.65f,           0.95f,  // 5 leftShoulder
            cx + scale * 0.20f,     cy - scale * 0.65f,           0.95f,  // 6 rightShoulder
            cx - scale * 0.25f,     cy - scale * 0.30f,           0.90f,  // 7 leftElbow
            cx + scale * 0.25f,     cy - scale * 0.30f,           0.90f,  // 8 rightElbow
            cx - scale * 0.28f,     cy + scale * 0.05f,           0.85f,  // 9 leftWrist
            cx + scale * 0.28f,     cy + scale * 0.05f,           0.85f,  // 10 rightWrist
            cx - scale * 0.15f,     cy + scale * 0.10f + hipDropY,0.95f,  // 11 leftHip
            cx + scale * 0.15f,     cy + scale * 0.10f + hipDropY,0.95f,  // 12 rightHip
            cx - scale * 0.18f,     cy + scale * 0.55f + hipDropY,0.90f,  // 13 leftKnee
            cx + scale * 0.18f,     cy + scale * 0.55f + hipDropY,0.90f,  // 14 rightKnee
            cx - scale * 0.16f,     cy + scale * 0.95f,           0.90f,  // 15 leftAnkle
            cx + scale * 0.16f,     cy + scale * 0.95f,           0.90f   // 16 rightAnkle
        )

        // Box around the whole figure
        val box = floatArrayOf(
            cx - scale * 0.35f,   // x1
            cy - scale * 1.00f,   // y1
            cx + scale * 0.35f,   // x2
            cy + scale * 1.00f    // y2
        )

        // Layout: [numPersons=1, box(4), keypoints(17×3)]
        return floatArrayOf(1f) + box + kps
    }

    // ── JNI declarations (only called when nativeLibLoaded = true) ────────────

    private external fun buildNetworkBB(dlcBuffer: ByteArray, size: Int, runtime: Char): Boolean
    private external fun buildNetworkPose(dlcBuffer: ByteArray, size: Int, runtime: Char): Boolean
    private external fun runInferenceNative(rgbaBytes: ByteArray, width: Int, height: Int): FloatArray
    private external fun destroyNative()

    private fun loadAsset(name: String): ByteArray? =
        runCatching { context.assets.open(name).use { it.readBytes() } }
            .onFailure { Log.w(TAG, "Asset '$name' not found: ${it.message}") }
            .getOrNull()
}

// Convenience operator so we can write  floatArrayOf(...) + floatArrayOf(...)
private operator fun FloatArray.plus(other: FloatArray): FloatArray {
    val result = FloatArray(size + other.size)
    copyInto(result)
    other.copyInto(result, size)
    return result
}
