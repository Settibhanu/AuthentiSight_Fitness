package com.fitness.snapapp.media.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps ExoPlayer for tutorial MP4 playback.
 * Videos are read from:
 *   [Context.getExternalFilesDir]/videos/<filename>.mp4
 *
 * No video is recorded or written — files are read-only at runtime.
 */
class VideoPlayer(private val context: Context) {

    private var player: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** The underlying ExoPlayer instance, for attaching to a PlayerView in Compose. */
    fun getPlayer(): ExoPlayer? = player

    fun prepare(videoFileName: String): Boolean {
        release()
        val file = context.getExternalFilesDir(null)
            ?.resolve("videos/$videoFileName") ?: return false
        if (!file.exists()) return false

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }
            })
            prepare()
        }
        return true
    }

    fun play()  { player?.play()  }
    fun pause() { player?.pause() }

    fun seekTo(positionMs: Long) { player?.seekTo(positionMs) }

    fun release() {
        player?.release()
        player = null
        _isPlaying.value = false
    }
}
