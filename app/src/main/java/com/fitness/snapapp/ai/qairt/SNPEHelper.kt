package com.fitness.snapapp.ai.qairt

import android.content.Context
import android.util.Log

/**
 * Kotlin-side SNPE entry point.
 *
 * Loads DLC bytes from APK assets and delegates all heavy inference to C++
 * via JNI (libsnpe_jni.so).
 *
 * Runtime options:
 *   'C' = CPU  (Oryon performance cores)
 *   'G' = GPU  (Adreno)
 *   'D' = DSP/HTP (Hexagon NPU V79) — RECOMMENDED for SM8750
 *
 * Thread safety:
 *   [runInference] must be called from the QAIRT Worker thread only
 *   (AppDispatchers.inference). The underlying SNPE execute() is NOT thread-safe.
 */
class SNPEHelper(private val context: Context) {

    companion object {
        private const val TAG = "SNPEHelper"

        init {
            System.loadLibrary("snpe_jni")
        }
    }

    private var initialized = false

    /**
     * Initialize both YOLO-NAS and HRNet networks.
     * Must be called once on app startup (before any [runInference] call).
     *
     * @param runtime  'C' = CPU, 'G' = GPU, 'D' = DSP/HTP (default)
     * @return true if both networks were built successfully
     */
    fun initialize(runtime: Char = 'D'): Boolean {
        val yoloDlc  = loadAsset("Quant_yoloNas_s_320.dlc")
        val hrnetDlc = loadAsset("hrnet_axis_int8.dlc")

        if (yoloDlc == null) {
            Log.e(TAG, "Could not load Quant_yoloNas_s_320.dlc from assets")
            return false
        }
        if (hrnetDlc == null) {
            Log.e(TAG, "Could not load hrnet_axis_int8.dlc from assets")
            return false
        }

        val bbOk   = buildNetworkBB(yoloDlc,   yoloDlc.size,   runtime)
        val poseOk = buildNetworkPose(hrnetDlc, hrnetDlc.size,  runtime)

        initialized = bbOk && poseOk
        Log.i(TAG, "SNPE init: detector=$bbOk pose=$poseOk runtime=$runtime → ok=$initialized")
        return initialized
    }

    /** @return true if [initialize] succeeded */
    fun isReady(): Boolean = initialized

    private fun loadAsset(name: String): ByteArray? =
        runCatching { context.assets.open(name).use { it.readBytes() } }
            .onFailure { Log.e(TAG, "Failed to read asset '$name': ${it.message}") }
            .getOrNull()

    // ── JNI declarations ─────────────────────────────────────────────────────

    /**
     * Build the YOLO-NAS-S bounding-box network from DLC bytes.
     * @param dlcBuffer  Raw bytes of the .dlc file
     * @param size       Length of dlcBuffer
     * @param runtime    'C', 'G', or 'D'
     */
    external fun buildNetworkBB(dlcBuffer: ByteArray, size: Int, runtime: Char): Boolean

    /**
     * Build the HRNet pose-estimation network from DLC bytes.
     */
    external fun buildNetworkPose(dlcBuffer: ByteArray, size: Int, runtime: Char): Boolean

    /**
     * Run the full two-stage inference pipeline on one RGBA frame.
     *
     * Stage 1: YOLO-NAS-S detects person bounding boxes.
     * Stage 2: For each detected person, crop + affine-warp to 256×192 → HRNet → 17 keypoints.
     *
     * @param rgbaBytes  RGBA byte array from CameraX ImageProxy (width×height×4 bytes)
     * @param width      Frame width in pixels
     * @param height     Frame height in pixels
     *
     * @return FloatArray with layout:
     *   [numPersons,
     *    box0_x1, box0_y1, box0_x2, box0_y2,
     *    kp0_x, kp0_y, kp0_conf, kp1_x, kp1_y, kp1_conf, ...(×17),
     *    box1_x1, ...]
     *   Returns empty array if no persons are detected or networks are not ready.
     */
    external fun runInference(rgbaBytes: ByteArray, width: Int, height: Int): FloatArray

    /**
     * Release all SNPE network resources.
     * Call from [onDestroy] / lifecycle cleanup.
     */
    external fun destroy()
}
