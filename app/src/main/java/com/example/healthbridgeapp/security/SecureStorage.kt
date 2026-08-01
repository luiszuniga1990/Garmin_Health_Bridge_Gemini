package com.example.healthbridgeapp.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "hb_secure_prefs"
private const val KEY_API_KEY = "gemini_api_key"

/**
 * Almacena la Gemini API key de forma segura en Android Keystore.
 * En Android 16, las claves son hardware-backed por defecto (TEE/StrongBox).
 * NUNCA almacenar API keys en texto plano o SharedPreferences normales.
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? = encryptedPrefs.getString(KEY_API_KEY, null)

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
    }
}
