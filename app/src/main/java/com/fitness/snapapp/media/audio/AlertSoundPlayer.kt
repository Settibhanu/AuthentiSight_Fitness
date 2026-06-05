package com.fitness.snapapp.media.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Thread 5 — Audio Engine
 *
 * Delivers real-time posture and rep-milestone alerts via Android TTS.
 * Fully on-device. No audio is recorded or stored.
 *
 * Rate-limited: will not repeat the same message within [MIN_INTERVAL_MS] ms.
 */
class AlertSoundPlayer(context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "AlertSoundPlayer"
    private val MIN_INTERVAL_MS = 3000L

    private val tts = TextToSpeech(context, this)
    private var ready = false
    private var lastMessage = ""
    private var lastSpokenAt = 0L

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(1.1f)
            tts.setPitch(1.0f)
            Log.i(TAG, "TTS initialised, ready=$ready")
        } else {
            Log.e(TAG, "TTS init failed with status=$status")
        }
    }

    /**
     * Speak [message] aloud. Will not repeat the same message within 3 seconds.
     * Queue flushed on each call so the latest alert is always heard immediately.
     */
    fun speak(message: String) {
        if (!ready) return
        val now = System.currentTimeMillis()
        if (message == lastMessage && (now - lastSpokenAt) < MIN_INTERVAL_MS) return
        lastMessage   = message
        lastSpokenAt  = now
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "alert_${now}")
    }

    /** Announce milestone rep counts: "10 reps!", "20 reps!" etc. */
    fun announceReps(count: Int) {
        if (count > 0 && count % 10 == 0) speak("$count reps!")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
