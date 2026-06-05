package com.fitness.snapapp.ai.avatar

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import com.fitness.snapapp.ai.pose.Pose
import com.google.android.filament.Engine
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer

/**
 * Thread 4 — Avatar Rendering (GPU via Filament / OpenGL ES 3.x)
 *
 * Workflow per frame:
 *   1. Inference pipeline calls [updatePose] with the current frame's Pose.
 *   2. Skeleton bones are driven from Pose keypoints.
 *   3. Filament renders one frame.
 *   4. Pose reference is nulled — frame is discarded.
 *
 * NO animation is stored. NO video is generated. Only a single pose reference
 * exists at any time — the previous one is immediately eligible for GC.
 *
 * GLB files must be placed at:  app/src/main/assets/avatars/male.glb
 *                                app/src/main/assets/avatars/female.glb
 */
class AvatarRenderer(
    private val context: Context,
    private val glSurfaceView: GLSurfaceView
) {
    private val TAG = "AvatarRenderer"

    /** Single-frame pose reference — replaced atomically each frame. */
    @Volatile private var currentPose: Pose? = null

    private var modelViewer: ModelViewer? = null
    private var isInitialized = false

    enum class AvatarGender { MALE, FEMALE }

    private var currentGender = AvatarGender.MALE

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call once from the owning Activity/Fragment after the GLSurfaceView is attached.
     * @param gender Which GLB to load from assets.
     */
    fun initialize(gender: AvatarGender = AvatarGender.MALE) {
        currentGender = gender
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(
                gl: javax.microedition.khronos.opengles.GL10?,
                config: javax.microedition.khronos.egl.EGLConfig?
            ) {
                val engine = Engine.create()
                modelViewer = ModelViewer(engine, glSurfaceView)
                loadAvatar(gender)
                isInitialized = true
                Log.i(TAG, "Filament engine initialised; avatar=${gender.name}")
            }

            override fun onSurfaceChanged(
                gl: javax.microedition.khronos.opengles.GL10?,
                width: Int, height: Int
            ) {
                modelViewer?.view?.viewport = com.google.android.filament.Viewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                val pose = currentPose
                if (pose != null) {
                    driveSkeletonFromPose(pose)
                    currentPose = null   // discard — frame over
                }
                modelViewer?.render(System.nanoTime())
            }
        })
    }

    /**
     * Called from the inference pipeline each frame.
     * Previous pose is immediately replaced — NOT accumulated.
     */
    fun updatePose(pose: Pose) {
        currentPose = pose
        glSurfaceView.requestRender()
    }

    fun switchAvatar(gender: AvatarGender) {
        if (gender == currentGender) return
        currentGender = gender
        glSurfaceView.queueEvent { loadAvatar(gender) }
    }

    fun destroy() {
        modelViewer?.destroyModel()
        isInitialized = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadAvatar(gender: AvatarGender) {
        val assetPath = if (gender == AvatarGender.MALE) "avatars/male.glb"
                        else                              "avatars/female.glb"
        try {
            val buffer = context.assets.open(assetPath).use { stream ->
                val bytes = stream.readBytes()
                ByteBuffer.wrap(bytes)
            }
            modelViewer?.loadModelGlb(buffer)
            modelViewer?.transformToUnitCube()
            Log.i(TAG, "Loaded avatar: $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load avatar '$assetPath': ${e.message}")
        }
    }

    /**
     * Maps COCO 17-keypoint Pose to the humanoid skeleton's bone transforms.
     *
     * The actual bone-name → joint-index mapping depends on the rig in your GLB.
     * Common naming conventions: "mixamorig:LeftArm", "Hips", etc.
     *
     * This method is a template — fill in your rig's bone names and the
     * corresponding COCO keypoint mappings below.
     */
    private fun driveSkeletonFromPose(pose: Pose) {
        val animator = modelViewer?.animator ?: return
        // Example: drive hip bone from leftHip + rightHip midpoint
        // val hipX = (pose.leftHip.x + pose.rightHip.x) / 2f
        // val hipY = (pose.leftHip.y + pose.rightHip.y) / 2f
        // animator.applyAnimation(hipBoneIndex, hipX, hipY, ...)
        //
        // Implement full IK or direct FK mapping here based on your GLB rig.
        // Filament's animator API: animator.applyAnimation(animIndex, time)
        // For direct bone manipulation: use TransformManager on the skeleton entity.
    }
}
