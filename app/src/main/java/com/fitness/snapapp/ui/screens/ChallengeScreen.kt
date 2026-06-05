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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fitness.snapapp.data.room.entity.ChallengeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(navController: NavController) {
    // Demo data — replace with ViewModel / ChallengeEngine in production
    val challenges = remember {
        listOf(
            ChallengeEntity(1, "100 Squats in a Week",  target = 100, progress = 40,  completed = false),
            ChallengeEntity(2, "30-Day Push-Up Streak", target = 30,  progress = 12,  completed = false),
            ChallengeEntity(3, "500 Total Reps",        target = 500, progress = 500, completed = true),
            ChallengeEntity(4, "Plank for 5 Minutes",   target = 300, progress = 80,  completed = false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController, currentRoute = "challenges") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text("Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(challenges.filter { !it.completed }) { challenge ->
                ChallengeCard(challenge = challenge)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(challenges.filter { it.completed }) { challenge ->
                ChallengeCard(challenge = challenge)
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: ChallengeEntity) {
    val progress = challenge.progress.toFloat() / challenge.target.toFloat()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.completed)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    challenge.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (challenge.completed) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (challenge.completed) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Text(
                "${challenge.progress} / ${challenge.target}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
