package com.example.healthbridgeapp

import android.app.Application
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.example.healthbridgeapp.ai.ClaudeClient
import com.example.healthbridgeapp.ai.GeminiClient
import com.example.healthbridgeapp.health.HealthConnectReader
import com.example.healthbridgeapp.security.AiProvider
import com.example.healthbridgeapp.security.SecureStorage

/**
 * Application class: punto central de inicialización de dependencias.
 * Mantiene instancias singleton de los clientes principales.
 */
class HealthBridgeApp : Application() {

    lateinit var secureStorage: SecureStorage
        private set

    val healthConnectClient: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(this)
        } catch (e: Exception) {
            null
        }
    }

    val healthConnectReader: HealthConnectReader? by lazy {
        healthConnectClient?.let { HealthConnectReader(it) }
    }

    fun createGeminiClient(): GeminiClient? {
        val apiKey = secureStorage.getGeminiApiKey() ?: secureStorage.getApiKey() ?: return null
        return GeminiClient(apiKey)
    }

    fun createClaudeClient(): ClaudeClient? {
        val apiKey = secureStorage.getClaudeApiKey() ?: secureStorage.getApiKey() ?: return null
        return ClaudeClient(apiKey)
    }

    override fun onCreate() {
        super.onCreate()
        secureStorage = SecureStorage(this)
    }

    companion object {
        fun from(context: Context): HealthBridgeApp =
            context.applicationContext as HealthBridgeApp
    }
}
