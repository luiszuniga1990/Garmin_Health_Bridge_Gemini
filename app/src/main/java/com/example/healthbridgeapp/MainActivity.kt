package com.example.healthbridgeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.lifecycleScope
import com.example.healthbridgeapp.ai.AIInsight
import com.example.healthbridgeapp.ui.DashboardScreen
import com.example.healthbridgeapp.ui.OnboardingScreen
import com.example.healthbridgeapp.ui.theme.HealthBridgeTheme
import kotlinx.coroutines.launch

// Todos los permisos de Health Connect que necesitamos
val HC_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(RespiratoryRateRecord::class),
)

class MainActivity : ComponentActivity() {

    private val app by lazy { HealthBridgeApp.from(this) }

    // Launcher para solicitar permisos de Health Connect
    private val requestPermissionsLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HC_PERMISSIONS)) {
            triggerDataLoad()
        }
    }

    // Estado observable del UI
    private val uiState = mutableStateOf<AppUiState>(AppUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthBridgeTheme {
                val state = uiState.value
                when (state) {
                    is AppUiState.NeedsApiKey -> OnboardingScreen(
                        onApiKeySubmitted = { key -> handleApiKeySaved(key) }
                    )
                    is AppUiState.Loading -> DashboardScreen(
                        insight = null,
                        isLoading = true,
                        onRefresh = { triggerDataLoad() }
                    )
                    is AppUiState.Ready -> DashboardScreen(
                        insight = state.insight,
                        isLoading = false,
                        onRefresh = { triggerDataLoad() }
                    )
                    is AppUiState.Error -> DashboardScreen(
                        insight = null,
                        isLoading = false,
                        errorMessage = state.message,
                        onRefresh = { triggerDataLoad() }
                    )
                }
            }
        }
        initializeApp()
    }

    private fun initializeApp() {
        if (!app.secureStorage.hasApiKey()) {
            uiState.value = AppUiState.NeedsApiKey
            return
        }
        val hcClient = app.healthConnectClient
        if (hcClient == null) {
            uiState.value = AppUiState.Error("Health Connect no disponible. Actualiza el sistema.")
            return
        }
        lifecycleScope.launch {
            val granted = hcClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(HC_PERMISSIONS)) {
                triggerDataLoad()
            } else {
                requestPermissionsLauncher.launch(HC_PERMISSIONS)
            }
        }
    }

    private fun triggerDataLoad() {
        uiState.value = AppUiState.Loading
        lifecycleScope.launch {
            try {
                val reader = app.healthConnectReader
                    ?: throw Exception("Health Connect no inicializado")
                val gemini = app.createGeminiClient()
                    ?: throw Exception("API key de Gemini no configurada")

                val snapshot = reader.readLastWeekSnapshot()
                val insight = gemini.analyzeHealthSnapshot(snapshot)
                uiState.value = AppUiState.Ready(insight)
            } catch (e: Exception) {
                uiState.value = AppUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun handleApiKeySaved(apiKey: String) {
        app.secureStorage.saveApiKey(apiKey)
        initializeApp()
    }
}

sealed class AppUiState {
    object NeedsApiKey : AppUiState()
    object Loading : AppUiState()
    data class Ready(val insight: AIInsight) : AppUiState()
    data class Error(val message: String) : AppUiState()
}
