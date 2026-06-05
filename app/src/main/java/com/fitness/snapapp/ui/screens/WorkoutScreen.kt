package com.fitness.snapapp.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fitness.snapapp.ui.viewmodel.WorkoutViewModel

/**
 * Core workout screen:
 *   - Full-screen CameraX preview (background)
 *   - Rep counter overlay (large, centred)
 *   - Posture alert banner (red, top)
 *   - Runtime selector toggle (CPU / GPU / DSP)
 *   - Exercise selector dropdown
 *   - Start / Pause / Finish buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    exercise: String,
    navController: NavController,
    vm: WorkoutViewModel = viewModel()
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val repCount    by vm.repCount.collectAsState()
    val postureAlert by vm.postureAlert.collectAsState()
    val isActive    by vm.isActive.collectAsState()
    val selectedRuntime by vm.runtime.collectAsState()

    var showExercisePicker by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf(exercise) }

    LaunchedEffect(exercise) {
        vm.setExercise(exercise)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layer 1: CameraX preview ──────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView ->
                    vm.startCamera(ctx, lifecycleOwner, previewView)
                }
            }
        )

        // ── Layer 2: Posture alert banner ─────────────────────────────────
        AnimatedVisibility(
            visible  = postureAlert != null,
            enter    = slideInVertically { -it },
            exit     = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
        ) {
            postureAlert?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Text(msg, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Layer 3: Top controls (back + runtime + exercise picker) ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                colors  = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Runtime selector
            RuntimeToggle(
                selected = selectedRuntime,
                onSelect = { vm.setRuntime(it) }
            )
        }

        // ── Layer 4: Rep counter (centre) ─────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text       = "$repCount",
                fontSize   = 96.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                modifier   = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
            Text(
                text     = selectedExercise.replace("_", " "),
                fontSize = 18.sp,
                color    = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }

        // ── Layer 5: Bottom controls ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Exercise selector
            OutlinedButton(
                onClick = { showExercisePicker = true },
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border  = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(selectedExercise.replace("_", " "))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            // Start / Pause / Finish row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    Button(
                        onClick = { vm.startWorkout(selectedExercise) },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.pauseWorkout() },
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }

                    Button(
                        onClick = {
                            vm.finishWorkout()
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Finish", color = Color.Black)
                    }
                }
            }
        }
    }

    // Exercise picker dialog
    if (showExercisePicker) {
        ExercisePickerDialog(
            onDismiss = { showExercisePicker = false },
            onSelect   = { ex ->
                selectedExercise = ex
                vm.setExercise(ex)
                showExercisePicker = false
            }
        )
    }
}

@Composable
private fun RuntimeToggle(selected: Char, onSelect: (Char) -> Unit) {
    val options = listOf('C' to "CPU", 'G' to "GPU", 'D' to "DSP")
    Row(
        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (key, label) ->
            TextButton(
                onClick = { onSelect(key) },
                colors  = ButtonDefaults.textButtonColors(
                    contentColor = if (selected == key) MaterialTheme.colorScheme.primary else Color.White
                ),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(label, fontSize = 12.sp, fontWeight = if (selected == key) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val exercises = listOf("SQUATS", "PUSH_UPS", "LUNGES", "PLANK", "JUMPING_JACKS", "SIT_UPS", "BURPEES")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                exercises.forEach { ex ->
                    TextButton(
                        onClick = { onSelect(ex) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(ex.replace("_", " "))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
