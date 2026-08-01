package com.example.healthbridgeapp.ai

import android.util.Log
import com.example.healthbridgeapp.health.HealthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "GeminiClient"

@Serializable
data class AIInsight(
    val estado: String = "DESCONOCIDO",          // ÓPTIMO | PRECAUCIÓN | ALARMA
    val emoji_estado: String = "⚪",              // 🟢 🟡 🔴
    val recomendacion_hoy: String = "",           // Qué hacer hoy
    val metrica_clave: String = "",               // Métrica más importante esta semana
    val proyeccion_semana: String = "",           // Proyección a 7 días
    val alerta: String? = null,                  // Alerta específica si existe
    val body_battery_estimado: Int = 0,           // 0-100
    val listo_para_correr: Boolean = false,       // ¿Puede correr hoy?
    val distancia_recomendada_km: Double = 0.0,  // km recomendados si corre
    val ritmo_recomendado: String = "",           // pace objetivo
)

/**
 * Cliente de Gemini para Android mediante REST API directo (HttpURLConnection).
 * Soporta cualquier formato de API Key (AIza... o AQ...) y modelos Gemini 1.5/2.0/3.0.
 */
class GeminiClient(private val apiKey: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun analyzeHealthSnapshot(snapshot: HealthSnapshot): AIInsight = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(snapshot)
        val modelsToTry = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-3.1-flash-lite-preview")
        var lastErrorMessage = ""

        for (modelName in modelsToTry) {
            try {
                Log.d(TAG, "Enviando análisis a Gemini ($modelName)...")
                val rawText = makeHttpRequest(modelName, prompt)
                Log.d(TAG, "Respuesta Gemini ($modelName): $rawText")
                return@withContext json.decodeFromString<AIInsight>(rawText)
            } catch (e: Exception) {
                Log.e(TAG, "Error con modelo $modelName: ${e.message}")
                lastErrorMessage = e.message ?: "Error desconocido"
            }
        }

        AIInsight(
            estado = "ERROR",
            emoji_estado = "⚠️",
            recomendacion_hoy = "Error al conectar con Gemini: $lastErrorMessage",
            metrica_clave = "Conectividad"
        )
    }

    private fun makeHttpRequest(modelName: String, promptText: String): String {
        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("x-goog-api-key", apiKey)
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        // Construir JSON payload
        val payload = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", promptText)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.4)
                put("maxOutputTokens", 1024)
                put("responseMimeType", "application/json")
            }
        }.toString()

        conn.outputStream.use { os ->
            os.write(payload.toByteArray(Charsets.UTF_8))
        }

        val statusCode = conn.responseCode
        val inputStream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

        if (statusCode !in 200..299) {
            throw Exception("HTTP $statusCode: $responseBody")
        }

        // Parsear candidates[0].content.parts[0].text
        val responseJson = json.parseToJsonElement(responseBody).jsonObject
        val candidates = responseJson["candidates"]?.jsonArray
            ?: throw Exception("Sin candidatos en respuesta: $responseBody")
        val firstCandidate = candidates.firstOrNull()?.jsonObject
            ?: throw Exception("Candidatos vacíos")
        val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
            ?: throw Exception("Sin contenido en respuesta")
        val rawText = parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw Exception("Texto de respuesta vacío")

        return rawText
    }

    private fun buildPrompt(data: HealthSnapshot): String {
        val exerciseCount = data.exerciseSessions.size
        val lastRunPaceMin = data.lastRunPaceSecPerKm / 60
        val lastRunPaceSec = data.lastRunPaceSecPerKm % 60

        return """
Eres el coach de fitness personal del usuario. Tu objetivo es evaluar el estado de recuperación de hoy y dar la recomendación exacta alineada con su CALENDARIO DE ENTRENAMIENTO SEMANAL.

MÉTRICAS EXTRAÍDAS DE HEALTH CONNECT (últimos 7 días):
- HRV promedio: ${String.format("%.1f", data.avgHrv)} ms | valores recientes: ${data.hrvValues.takeLast(3).joinToString { String.format("%.0f", it) }} ms
- Sueño anoche: ${String.format("%.1f", data.lastSleepHours)}h (Profundo: ${String.format("%.1f", data.deepSleepHours)}h, REM: ${String.format("%.1f", data.remSleepHours)}h)
- Score de sueño calculado: ${data.sleepScore}/100
- Pasos hoy: ${data.stepsToday} | Pasos 7 días: ${data.steps7Days}
- FC Reposo estimada: ${data.heartRateResting} bpm | SpO2: ${String.format("%.1f", data.spo2Latest)}%
- Ejercicios registrados esta semana: $exerciseCount sesiones (${String.format("%.1f", data.distanceWeekKm)} km totales, ${data.caloriesWeek} kcal)
- Última corrida — Pace: ${lastRunPaceMin}:${String.format("%02d", lastRunPaceSec)} /km | FC Media: ${data.lastRunHrBpm} bpm

PERFIL Y CALENDARIO SEMANAL OBJETIVO DEL USUARIO:
- LUNES: 💪 Fuerza (55 min)
- MARTES: 💪 Fuerza (50 min)
- MIÉRCOLES: 🚴 Ciclismo Indoor Z2 (30-35 min)
- JUEVES: 💪 Fuerza + 🏃 Corrida Z3 (5-6 km) [Día Doble]
- VIERNES: 💪 Fuerza o Descanso
- SÁBADO: 💪 Fuerza + 🏊 Natación (30-40 min)
- DOMINGO: 🏃 Corrida (5.5-6.5 km) + 🏊 Natación Crioterapia (30 min)

REGLAS DE AUTORREGULACIÓN POR HRV (Baseline del usuario: 111-119 ms):
- HRV > 115 ms (🟢 ÓPTIMO): Ejecutar plan del día al 100%.
- HRV 105-115 ms (🟡 PRECAUCIÓN): Ejecutar plan del día sin añadir volumen extra.
- HRV 95-105 ms (🟠 ATENCIÓN): Reducir running/cycling al 70%. Fuerza normal.
- HRV < 95 ms o Sueño < 60 (🔴 ALARMA): Descanso activo / movilidad Zona 1.

METAS BIOMECÁNICAS:
- Cadencia carrera: Meta 170-175 spm (actual 168 spm)
- Contacto suelo: Meta < 250 ms (actual ~272 ms)
- Oscilación vertical: Meta 6-8 cm (actual 8.5 cm)

Determina el día de la semana según los datos y responde ÚNICAMENTE con este JSON exacto:
{
  "estado": "ÓPTIMO|PRECAUCIÓN|ALARMA",
  "emoji_estado": "🟢|🟡|🔴",
  "recomendacion_hoy": "recomendación específica según el día de la semana y el HRV de hoy en 1-2 oraciones",
  "metrica_clave": "métrica biomecánica o de descanso más crítica hoy",
  "proyeccion_semana": "proyección de carga para los próximos 7 días",
  "alerta": "alerta específica si el HRV o el sueño están comprometidos, o null",
  "body_battery_estimado": 60,
  "listo_para_correr": true o false,
  "distancia_recomendada_km": número decimal de km según el plan del día,
  "ritmo_recomendado": "X:XX /km o vacío"
}
        """.trimIndent()
    }
}
