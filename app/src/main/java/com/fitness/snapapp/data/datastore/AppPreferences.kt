package com.fitness.snapapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fitness.snapapp.core.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * DataStore wrapper for user-adjustable app settings.
 * Allowed: runtime preference, avatar gender, onboarding state.
 * FORBIDDEN: any pose data, camera frame data.
 */
class AppPreferences(private val context: Context) {

    private object Keys {
        val RUNTIME         = stringPreferencesKey(AppConstants.PREF_RUNTIME)
        val AVATAR_GENDER   = stringPreferencesKey(AppConstants.PREF_AVATAR_GENDER)
        val ONBOARDING_DONE = booleanPreferencesKey(AppConstants.PREF_ONBOARDING_DONE)
    }

    /** 'C' = CPU, 'G' = GPU, 'D' = DSP/HTP */
    val runtime: Flow<Char> = context.dataStore.data
        .map { prefs -> (prefs[Keys.RUNTIME] ?: "D").first() }

    /** "MALE" or "FEMALE" */
    val avatarGender: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.AVATAR_GENDER] ?: "MALE" }

    val onboardingDone: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setRuntime(r: Char) {
        context.dataStore.edit { prefs -> prefs[Keys.RUNTIME] = r.toString() }
    }

    suspend fun setAvatarGender(gender: String) {
        context.dataStore.edit { prefs -> prefs[Keys.AVATAR_GENDER] = gender }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE] = done }
    }
}
