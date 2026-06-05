package com.fitness.snapapp.media.music

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Background workout music player (MP3/AAC).
 * Audio files are read from:
 *   [Context.getExternalFilesDir]/music/<filename>
 *
 * Fully offline — no streaming. No audio is recorded or stored.
 */
class MusicPlayer(private val context: Context) {

    private var player: ExoPlayer? = null

    fun playAll(shuffled: Boolean = false) {
        release()
        val musicDir = context.getExternalFilesDir(null)?.resolve("music") ?: return
        val files = musicDir.listFiles { f -> f.extension in listOf("mp3", "aac", "m4a") }
            ?.sortedBy { it.name } ?: return

        if (files.isEmpty()) return

        val items = (if (shuffled) files.shuffled() else files)
            .map { MediaItem.fromUri(Uri.fromFile(it)) }

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItems(items)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            play()
        }
    }

    fun playFile(fileName: String) {
        release()
        val file = context.getExternalFilesDir(null)?.resolve("music/$fileName")
            ?.takeIf(File::exists) ?: return
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            play()
        }
    }

    fun pause()  { player?.pause() }
    fun resume() { player?.play()  }

    fun setVolume(volume: Float) { player?.volume = volume.coerceIn(0f, 1f) }

    fun release() {
        player?.release()
        player = null
    }
}
