package com.thecityandthebike.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingPrefsTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var onboardingPrefs: OnboardingPrefs

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        sharedPreferences = mockk {
            every { getBoolean("completed", false) } returns false
            every { edit() } returns editor
        }
        context = mockk {
            every { getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        }
        onboardingPrefs = OnboardingPrefs(context)
    }

    @Test
    fun `isOnboardingCompleted returns false by default`() {
        assertFalse(onboardingPrefs.isOnboardingCompleted())
    }

    @Test
    fun `isOnboardingCompleted returns true when completed`() {
        every { sharedPreferences.getBoolean("completed", false) } returns true
        assertTrue(onboardingPrefs.isOnboardingCompleted())
    }

    @Test
    fun `setOnboardingCompleted writes true to shared preferences`() {
        every { editor.putBoolean("completed", true) } returns editor

        onboardingPrefs.setOnboardingCompleted()

        verify { editor.putBoolean("completed", true) }
        verify { editor.apply() }
    }
}
