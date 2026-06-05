package com.fitness.snapapp.domain.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.fitness.snapapp.media.audio.AlertSoundPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread 5 — Voice Assistant
 *
 * Combines:
 *  - On-device Whisper Tiny STT (via whisper.cpp JNI or SNPE DLC — pluggable)
 *  - Android TTS for spoken feedback
 *
 * AUDIO PRIVACY:
 *   - Audio is captured in short rolling buffers (< 5 seconds each).
 *   - Buffers are processed in RAM and NEVER written to disk.
 *   - No audio file is created at any point.
 *
 * Recognised commands (extend as needed):
 *   "start"  → onStart()
 *   "stop"   → onStop()
 *   "reset"  → onReset()
 *   "pause"  → onPause()
 *   "resume" → onResume()
 */
class VoiceAssistantController(
    private val context: Context,
    private val alertPlayer: AlertSoundPlayer
) {
    private val TAG = "VoiceAssistant"

    private val _recognizedCommand = MutableStateFlow<String?>(null)
    val recognizedCommand: StateFlow<String?> = _recognizedCommand.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Audio recording config
    private val sampleRate     = 16000
    private val channelConfig  = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat    = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize     = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        .coerceAtLeast(sampleRate * 2)  // at least 1 second

    // ── Public API ────────────────────────────────────────────────────────────

    fun startListening() {
        if (_isListening.value) return
        listenJob = scope.launch {
            _isListening.value = true
            recordAndProcess()
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        _isListening.value = false
    }

    fun speak(text: String) = alertPlayer.speak(text)

    fun destroy() {
        stopListening()
        scope.cancel()
    }

    // ── Private recording loop ────────────────────────────────────────────────

    private suspend fun recordAndProcess() = withContext(Dispatchers.Default) {
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "RECORD_AUDIO permission not granted: ${e.message}")
            _isListening.value = false
            return@withContext
        }

        try {
            audioRecord.startRecording()
            Log.i(TAG, "Voice recording started")
            val shortBuffer = ShortArray(bufferSize / 2)

            while (isActive && _isListening.value) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    // Convert to float PCM for Whisper
                    val floatBuffer = FloatArray(read) { shortBuffer[it] / 32768f }
                    // TODO: pass floatBuffer to Whisper.cpp JNI for transcription
                    // val transcript = WhisperJNI.transcribe(floatBuffer)
                    // processTranscript(transcript)
                    // Buffer is local — NOT stored after this iteration
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            Log.i(TAG, "Voice recording stopped")
        }
    }

    private fun processTranscript(text: String) {
        val command = text.trim().lowercase()
        val matched = when {
            command.contains("start")  -> "start"
            command.contains("stop")   -> "stop"
            command.contains("reset")  -> "reset"
            command.contains("pause")  -> "pause"
            command.contains("resume") -> "resume"
            else                       -> null
        }
        if (matched != null) {
            _recognizedCommand.value = matched
            Log.i(TAG, "Voice command: $matched")
        }
    }
}
