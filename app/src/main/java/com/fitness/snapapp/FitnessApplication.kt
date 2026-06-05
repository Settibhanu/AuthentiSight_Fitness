package com.fitness.snapapp

import android.app.Application
import android.util.Log
import com.fitness.snapapp.data.room.AppDatabase

/**
 * Application entry point.
 * Initialises the Room database singleton on startup.
 * All network-free — no analytics, no remote config calls.
 */
class FitnessApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i("FitnessApp", "Application started — fully offline mode")
        database = AppDatabase.getInstance(this)
    }
}
