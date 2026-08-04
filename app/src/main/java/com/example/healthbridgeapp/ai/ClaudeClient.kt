package com.example.healthbridgeapp.ai

import android.util.Log
import com.example.healthbridgeapp.health.HealthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ClaudeClient"

/**
 * Cliente nativo para Anthropic Claude REST API (v1/messages).
 * Permite hacer consultas directas a los modelos de Claude Sonnet / Haiku.
 */
class ClaudeClient(private val apiKey: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun analyzeHealthSnapshot(snapshot: HealthSnapshot): AIInsight = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(snapshot)
        val modelsToTry = listOf("claude-3-5-haiku-20241022", "claude-3-5-sonnet-20241022", "claude-3-7-sonnet-20250219")
        var lastErrorMessage = ""

        for (modelName in modelsToTry) {
            try {
                Log.d(TAG, "Enviando análisis a Anthropic Claude ($modelName)...")
                val rawText = makeHttpRequest(modelName, prompt)
                Log.d(TAG, "Respuesta Claude ($modelName): $rawText")
                return@withContext json.decodeFromString<AIInsight>(rawText)
            } catch (e: Exception) {
                Log.e(TAG, "Error con modelo $modelName: ${e.message}")
                lastErrorMessage = e.message ?: "Error desconocido"
            }
        }

        AIInsight(
            estado = "ERROR",
            emoji_estado = "⚠️",
            recomendacion_hoy = "Error al conectar con Claude API: $lastErrorMessage",
            metrica_clave = "Conectividad Anthropic"
        )
    }

    private fun makeHttpRequest(modelName: String, promptText: String): String {
        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 15000

        val payload = buildJsonObject {
            put("model", modelName)
            put("max_tokens", 1024)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", promptText)
                }
            }
        }.toString()

        conn.outputStream.use { os ->
            os.write(payload.toByteArray(Charsets.UTF_8))
        }

        val statusCode = conn.responseCode
        val inputStream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

        if (statusCode !in 200..299) {
            throw Exception("Anthropic API HTTP $statusCode: $responseBody")
        }

        val responseJson = json.parseToJsonElement(responseBody).jsonObject
        val contentArray = responseJson["content"]?.jsonArray
            ?: throw Exception("Sin array de contenido en respuesta de Claude: $responseBody")
        val textBlock = contentArray.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw Exception("Bloque de texto vacío en respuesta de Claude")

        // Extraer JSON en caso de que esté envuelto en bloques de código markdown ```json ... ```
        val jsonCleaned = textBlock.substringAfter("```json").substringBefore("```").trim().ifEmpty { textBlock.trim() }

        return jsonCleaned
    }

    private fun buildPrompt(data: HealthSnapshot): String {
        val exerciseCount = data.exerciseSessions.size
        val lastRunPaceMin = data.lastRunPaceSecPerKm / 60
        val lastRunPaceSec = data.lastRunPaceSecPerKm % 60

        return """
Eres el coach de fitness personal del usuario impulsado por Claude AI. Tu objetivo es evaluar sus métricas reales de salud y descanso para determinar:
1. SI DEBE O NO ENTRENAR LA RUTINA PROGRAMADA PARA HOY (si durmió mal o el HRV bajó, recomiéndale ajustar o suspender la rutina de hoy).
2. SI EXISTE RIESGO DE SOBREENTRENAMIENTO (evalúa la tendencia de HRV 7 días, FC en reposo, sueño y volumen de ejercicio acumulado).

MÉTRICAS EXTRAÍDAS DE GARMIN VIA HEALTH CONNECT (últimos 7 días):
- HRV promedio 7d: ${String.format("%.1f", data.avgHrv)} ms | HRV recientes: ${data.hrvValues.takeLast(3).joinToString { String.format("%.0f", it) }} ms (Baseline del atleta: 111-119 ms)
- Sueño anoche: ${String.format("%.1f", data.lastSleepHours)}h (Profundo: ${String.format("%.1f", data.deepSleepHours)}h, REM: ${String.format("%.1f", data.remSleepHours)}h)
- Score de sueño: ${data.sleepScore}/100
- Pasos hoy: ${data.stepsToday} | Pasos 7 días: ${data.steps7Days}
- FC Reposo estimada (RHR): ${data.heartRateResting} bpm | SpO2: ${String.format("%.1f", data.spo2Latest)}%
- Ejercicios registrados esta semana: $exerciseCount sesiones (${String.format("%.1f", data.distanceWeekKm)} km totales, ${data.caloriesWeek} kcal)
- Última corrida — Pace: ${lastRunPaceMin}:${String.format("%02d", lastRunPaceSec)} /km | FC Media: ${data.lastRunHrBpm} bpm

CALENDARIO DE ENTRENAMIENTO SEMANAL OBJETIVO:
- LUNES: 💪 Fuerza Upper (Push) - 55 min
- MARTES: 💪 Fuerza Lower (Piernas) - 50 min
- MIÉRCOLES: 🚴 Ciclismo Indoor Z2 - 35 min
- JUEVES: 💪 Fuerza (Pull) + 🏃 Corrida Z3 (5-6 km) [Día Doble]
- VIERNES: 💪 Fuerza Funcional / Descanso
- SÁBADO: 💪 Fuerza + 🏊 Natación (30-40 min)
- DOMINGO: 🏃 Corrida (5.5-6.5 km) + 🏊 Natación (30 min)

REGLAS DE EVALUACIÓN Y AJUSTE DE ENTRENAMIENTO:
- Si el sueño fue malo (< 6.5 horas o score < 65) o HRV < 105 ms: RECOMENDAR AJUSTAR O CANCELAR EL ENTRENAMIENTO DE HOY (ej: "Reducir peso/volumen un 30%" o "Sustituir por descanso activo").
- Si HRV < 95 ms o FC Reposo se elevó > 5 bpm: ALERTA DE SOBREENTRENAMIENTO. Recomendar descanso total.
- Si HRV 111-119 ms y Sueño > 7.5h (score > 80): Estado ÓPTIMO, entrenar al 100%.

Responde ÚNICAMENTE con el siguiente objeto JSON puro (sin texto adicional fuera del JSON):
{
  "estado": "ÓPTIMO|PRECAUCIÓN|ALARMA",
  "emoji_estado": "🟢|🟡|🔴",
  "recomendacion_hoy": "evaluación directa de su recuperación de hoy y si debe o no entrenar la rutina del día en 1-2 frases claras por Claude",
  "metrica_clave": "métrica de recuperación más crítica hoy",
  "proyeccion_semana": "proyección de carga de la semana considerando riesgo de sobreentrenamiento",
  "alerta": "alerta específica si durmió mal o hay riesgo de sobreentrenamiento, o null",
  "body_battery_estimado": 85,
  "listo_para_correr": true,
  "distancia_recomendada_km": 5.0,
  "ritmo_recomendado": "5:45 /km",
  "cadencia_actual_spm": 168,
  "contacto_suelo_ms": 272,
  "oscilacion_vertical_cm": 8.5,
  "vo2_max_estimado": 48.0,
  "actividad_hoy_nombre": "Fuerza Upper",
  "riesgo_sobreentrenamiento": "BAJO|MODERADO|ALTO",
  "estado_sueño_descanso": "ÓPTIMO|PRECAUCIÓN|INSUFICIENTE",
  "ajuste_entrenamiento_hoy": "Entrenar al 100%|Reducir carga 30%|Descanso Activo|Suspender Rutina"
}
""".trimIndent()
    }
}
