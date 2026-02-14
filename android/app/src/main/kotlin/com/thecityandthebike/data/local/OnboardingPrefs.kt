package com.thecityandthebike.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted() {
        sharedPreferences.edit { putBoolean(KEY_COMPLETED, true) }
    }

    companion object {
        private const val PREFS_FILENAME = "onboarding_prefs"
        private const val KEY_COMPLETED = "completed"
    }
}
