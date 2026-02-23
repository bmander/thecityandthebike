package com.thecityandthebike.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        createEncryptedPrefs()
    } catch (e: Exception) {
        // Corrupted prefs (e.g. signing key changed) — clear and retry
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE).edit { clear() }
        createEncryptedPrefs()
    }

    private fun createEncryptedPrefs(): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveToken(token: String) {
        sharedPreferences.edit { putString(KEY_TOKEN, token) }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit {
            putString(KEY_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        _isLoggedIn.value = true
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveIsAdmin(isAdmin: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_IS_ADMIN, isAdmin) }
    }

    fun getIsAdmin(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_ADMIN, false)
    }

    fun clearToken() {
        sharedPreferences.edit {
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_IS_ADMIN)
        }
        _isLoggedIn.value = false
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun getUserId(): String? {
        val token = getToken() ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            JSONObject(String(decoded)).getString("sub")
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_FILENAME = "secure_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_IS_ADMIN = "is_admin"
    }
}
