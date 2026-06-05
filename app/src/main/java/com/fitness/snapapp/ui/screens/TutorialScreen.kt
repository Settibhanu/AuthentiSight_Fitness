package com.fitness.snapapp.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.fitness.snapapp.media.video.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(exercise: String, navController: NavController) {
    val context = LocalContext.current
    val videoPlayer = remember { VideoPlayer(context) }
    val fileName = "${exercise.lowercase()}.mp4"

    DisposableEffect(Unit) {
        videoPlayer.prepare(fileName)
        onDispose { videoPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${exercise.replace("_", " ")} Tutorial",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ExoPlayer / Media3 PlayerView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer.getPlayer()
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(onClick = { videoPlayer.seekTo(0) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Rewind to start")
                }
                FilledIconButton(onClick = { videoPlayer.play() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
                FilledIconButton(onClick = { videoPlayer.pause() }) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start workout button
            Button(
                onClick = { navController.navigate("workout/$exercise") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start ${exercise.replace("_", " ")}")
            }
        }
    }
}
