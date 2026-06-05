package com.fitness.snapapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val exercises = listOf("SQUATS", "PUSH_UPS", "LUNGES", "PLANK", "JUMPING_JACKS", "SIT_UPS", "BURPEES")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDay   by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        },
        bottomBar = { BottomNavigationBar(navController, currentRoute = "schedule") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(daysOfWeek.indices.toList()) { dayIndex ->
                DayScheduleCard(
                    day       = daysOfWeek[dayIndex],
                    exercises = emptyList(), // TODO: wire to ScheduleEngine
                    onAddClick = { selectedDay = dayIndex; showAddDialog = true }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            dayName    = daysOfWeek[selectedDay],
            onDismiss  = { showAddDialog = false },
            onConfirm  = { exercise, reps -> showAddDialog = false }
        )
    }
}

@Composable
private fun DayScheduleCard(
    day: String,
    exercises: List<String>,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add for $day")
                }
            }
            if (exercises.isEmpty()) {
                Text("Rest day", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                exercises.forEach { ex ->
                    Text("• ${ex.replace("_", " ")}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AddScheduleDialog(
    dayName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedExercise by remember { mutableStateOf("SQUATS") }
    var targetReps by remember { mutableStateOf("20") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add for $dayName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedExercise.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        exercises.forEach { ex ->
                            DropdownMenuItem(
                                text = { Text(ex.replace("_", " ")) },
                                onClick = { selectedExercise = ex; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = { targetReps = it.filter(Char::isDigit) },
                    label = { Text("Target Reps") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedExercise, targetReps.toIntOrNull() ?: 10)
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
