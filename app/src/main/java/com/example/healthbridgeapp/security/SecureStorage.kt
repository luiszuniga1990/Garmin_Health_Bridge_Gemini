package com.example.healthbridgeapp.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "hb_secure_prefs"
private const val KEY_GEMINI_API_KEY = "gemini_api_key"
private const val KEY_CLAUDE_API_KEY = "claude_api_key"
private const val KEY_SELECTED_PROVIDER = "selected_ai_provider"

enum class AiProvider { GEMINI, CLAUDE }

/**
 * Almacena las API keys de Gemini y Claude de forma segura en Android Keystore.
 * En Android 16, las claves son hardware-backed por defecto (TEE/StrongBox).
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
        if (apiKey.startsWith("sk-ant-")) {
            saveClaudeApiKey(apiKey)
            saveSelectedProvider(AiProvider.CLAUDE)
        } else {
            saveGeminiApiKey(apiKey)
            saveSelectedProvider(AiProvider.GEMINI)
        }
    }

    fun saveGeminiApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun getGeminiApiKey(): String? = encryptedPrefs.getString(KEY_GEMINI_API_KEY, null)

    fun saveClaudeApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_CLAUDE_API_KEY, apiKey).apply()
    }

    fun getClaudeApiKey(): String? = encryptedPrefs.getString(KEY_CLAUDE_API_KEY, null)

    fun getApiKey(): String? = when (getSelectedProvider()) {
        AiProvider.CLAUDE -> getClaudeApiKey() ?: getGeminiApiKey()
        AiProvider.GEMINI -> getGeminiApiKey() ?: getClaudeApiKey()
    }

    fun hasApiKey(): Boolean = !getGeminiApiKey().isNullOrBlank() || !getClaudeApiKey().isNullOrBlank()

    fun saveSelectedProvider(provider: AiProvider) {
        encryptedPrefs.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
    }

    fun getSelectedProvider(): AiProvider {
        val name = encryptedPrefs.getString(KEY_SELECTED_PROVIDER, AiProvider.GEMINI.name)
        return try {
            AiProvider.valueOf(name ?: AiProvider.GEMINI.name)
        } catch (e: Exception) {
            AiProvider.GEMINI
        }
    }

    fun clearApiKeys() {
        encryptedPrefs.edit()
            .remove(KEY_GEMINI_API_KEY)
            .remove(KEY_CLAUDE_API_KEY)
            .remove(KEY_SELECTED_PROVIDER)
            .apply()
    }
}
