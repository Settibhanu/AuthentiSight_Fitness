package com.fitness.snapapp.ai.avatar

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import com.fitness.snapapp.ai.pose.Pose
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
 * GLB files must be at: app/src/main/assets/avatars/male.glb  /  female.glb
 */
class AvatarRenderer(
    private val context: Context,
    private val glSurfaceView: GLSurfaceView
) {
    private val TAG = "AvatarRenderer"

    /** Single-frame pose reference — replaced atomically each frame, never accumulated. */
    @Volatile private var currentPose: Pose? = null

    private var isInitialized = false

    enum class AvatarGender { MALE, FEMALE }
    private var currentGender = AvatarGender.MALE

    // Filament objects — lazily created on the GL thread
    private var filamentEngine: com.google.android.filament.Engine? = null
    private var modelViewer: com.google.android.filament.utils.ModelViewer? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun initialize(gender: AvatarGender = AvatarGender.MALE) {
        currentGender = gender
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(
                gl: javax.microedition.khronos.opengles.GL10?,
                config: javax.microedition.khronos.egl.EGLConfig?
            ) {
                try {
                    filamentEngine = com.google.android.filament.Engine.create()
                    filamentEngine?.let { engine ->
                        modelViewer = com.google.android.filament.utils.ModelViewer(
                            engine = engine,
                            surfaceView = glSurfaceView
                        )
                        loadAvatar(gender)
                        isInitialized = true
                        Log.i(TAG, "Filament engine initialised; avatar=${gender.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Filament init failed: ${e.message}")
                }
            }

            override fun onSurfaceChanged(
                gl: javax.microedition.khronos.opengles.GL10?,
                width: Int,
                height: Int
            ) {
                modelViewer?.view?.viewport =
                    com.google.android.filament.Viewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                val pose = currentPose
                if (pose != null) {
                    driveSkeletonFromPose(pose)
                    currentPose = null  // discard — frame over
                }
                modelViewer?.render(System.nanoTime())
            }
        })
    }

    /**
     * Called from the inference pipeline each frame.
     * The previous pose is replaced immediately — NOT accumulated.
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
        filamentEngine?.destroy()
        isInitialized = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadAvatar(gender: AvatarGender) {
        val assetPath = if (gender == AvatarGender.MALE) "avatars/male.glb"
                        else                              "avatars/female.glb"
        runCatching {
            context.assets.open(assetPath).use { stream ->
                val bytes = stream.readBytes()
                modelViewer?.loadModelGlb(ByteBuffer.wrap(bytes))
                modelViewer?.transformToUnitCube()
                Log.i(TAG, "Loaded avatar: $assetPath")
            }
        }.onFailure {
            Log.w(TAG, "Avatar '$assetPath' not found — avatar overlay disabled. ${it.message}")
        }
    }

    /**
     * Maps COCO 17-keypoint Pose to the humanoid skeleton bone transforms.
     *
     * Fill in your rig's actual bone names below.
     * Filament TransformManager API:
     *   val tm = engine.transformManager
     *   val instance = tm.getInstance(boneEntity)
     *   tm.setTransform(instance, Mat4.rotation(...))
     */
    private fun driveSkeletonFromPose(pose: Pose) {
        // Template — implement FK/IK based on your GLB rig
        // Example (pseudocode):
        //   val hipMidX = (pose.leftHip.x + pose.rightHip.x) / 2f
        //   val hipMidY = (pose.leftHip.y + pose.rightHip.y) / 2f
        //   applyBoneTransform("Hips", hipMidX, hipMidY)
    }
}
