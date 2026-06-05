package com.fitness.snapapp

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.fitness.snapapp.core.utils.PermissionUtils
import com.fitness.snapapp.ui.navigation.AppNavGraph
import com.fitness.snapapp.ui.screens.PermissionRationaleScreen
import com.fitness.snapapp.ui.theme.FitnessAppTheme

class MainActivity : ComponentActivity() {

    private var permissionsGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsGranted = PermissionUtils.allGranted(this)

        setContent {
            FitnessAppTheme {
                Surface {
                    if (permissionsGranted) {
                        val navController = rememberNavController()
                        AppNavGraph(navController = navController)
                    } else {
                        PermissionRationaleScreen(
                            onRequestPermissions = {
                                permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
                            }
                        )
                    }
                }
            }
        }
    }
}
