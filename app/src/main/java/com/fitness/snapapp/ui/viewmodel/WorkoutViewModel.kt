package com.fitness.snapapp.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.snapapp.ai.camera.CameraManager
import com.fitness.snapapp.ai.counter.ExerciseCounter
import com.fitness.snapapp.ai.pose.PoseExtractor
import com.fitness.snapapp.ai.qairt.SNPEHelper
import com.fitness.snapapp.core.threading.AppDispatchers
import com.fitness.snapapp.media.audio.AlertSoundPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel wiring:
 *   Thread 1 (CameraManager) → onFrameAvailable → Thread 2 (QAIRT) →
 *   Thread 3 (exercise logic) → StateFlow → UI thread
 */
class WorkoutViewModel : ViewModel() {

    private val TAG = "WorkoutViewModel"

    // ── Observable state ─────────────────────────────────────────────────────
    private val _repCount     = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _postureAlert = MutableStateFlow<String?>(null)
    val postureAlert: StateFlow<String?> = _postureAlert.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _runtime = MutableStateFlow('D')
    val runtime: StateFlow<Char> = _runtime.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────
    private var snpeHelper: SNPEHelper? = null
    private var cameraManager: CameraManager? = null
    private var alertPlayer: AlertSoundPlayer? = null
    private var exerciseCounter: ExerciseCounter? = null
    private var workoutStartMs = 0L

    private var currentExercise = ExerciseCounter.ExerciseType.SQUATS

    // ── Public API ────────────────────────────────────────────────────────────

    fun setExercise(name: String) {
        currentExercise = ExerciseCounter.ExerciseType.fromString(name)
    }

    fun setRuntime(r: Char) {
        _runtime.value = r
        // Re-init SNPE with new runtime if already running
        snpeHelper?.let { helper ->
            viewModelScope.launch(AppDispatchers.inference) {
                helper.initialize(r)
            }
        }
    }

    /**
     * Called once from WorkoutScreen when the PreviewView is ready.
     */
    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (snpeHelper == null) {
            snpeHelper = SNPEHelper(context).also { helper ->
                viewModelScope.launch(AppDispatchers.inference) {
                    val ok = helper.initialize(_runtime.value)
                    Log.i(TAG, "SNPE init result: $ok")
                }
            }
        }

        if (alertPlayer == null) {
            alertPlayer = AlertSoundPlayer(context)
        }

        cameraManager?.shutdown()
        cameraManager = CameraManager(context) { imageProxy ->
            // Thread 1 → frame arrives here
            val bytes  = imageProxy.planes[0].buffer.let { buf ->
                ByteArray(buf.remaining()).also { buf.get(it) }
            }
            val width  = imageProxy.width
            val height = imageProxy.height
            imageProxy.close()   // MUST close — frame data not stored

            if (!_isActive.value) return@CameraManager

            // Thread 2: QAIRT inference
            viewModelScope.launch(AppDispatchers.inference) {
                val output = snpeHelper?.runInference(bytes, width, height) ?: return@launch
                if (output.isEmpty()) return@launch

                // Thread 3: decode pose + exercise logic
                withContext(AppDispatchers.compute) {
                    val pose = PoseExtractor.fromInferenceOutput(output) ?: return@withContext
                    val counter = exerciseCounter ?: return@withContext

                    val reps = counter.process(pose)
                    _repCount.value = reps

                    // Posture alert (pose discarded after this call)
                    val alert = buildPostureAlert(pose)
                    _postureAlert.value = alert

                    // Thread 5: TTS alert
                    alert?.let { alertPlayer?.speak(it) }
                    alertPlayer?.announceReps(reps)
                }
            }
        }.also { it.startCamera(lifecycleOwner, previewView) }
    }

    fun startWorkout(exerciseName: String) {
        currentExercise = ExerciseCounter.ExerciseType.fromString(exerciseName)
        exerciseCounter = ExerciseCounter(currentExercise)
        workoutStartMs  = System.currentTimeMillis()
        _repCount.value = 0
        _postureAlert.value = null
        _isActive.value = true
    }

    fun pauseWorkout()  { _isActive.value = false }
    fun resumeWorkout() { _isActive.value = true  }

    fun finishWorkout() {
        _isActive.value = false
        // WorkoutManager.finishWorkout() would be called here with the DB
        // For now reset counters
        _repCount.value     = 0
        _postureAlert.value = null
        exerciseCounter?.reset()
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager?.shutdown()
        snpeHelper?.destroy()
        alertPlayer?.shutdown()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildPostureAlert(pose: com.fitness.snapapp.ai.pose.Pose): String? {
        // Delegate to PostureDetector (inline import to avoid circular reference in ViewModel)
        val detector = com.fitness.snapapp.ai.posture.PostureDetector()
        val result   = detector.checkPosture(pose, currentExercise.name)
        return if (!result.isCorrect) result.feedbackMessage else null
    }
}
