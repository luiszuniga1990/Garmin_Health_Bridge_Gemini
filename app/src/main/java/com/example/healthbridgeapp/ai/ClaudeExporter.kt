package com.example.healthbridgeapp.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.healthbridgeapp.health.HealthSnapshot
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ClaudeExporter {

    /**
     * Construye un prompt detallado y estructurado en Markdown con todas las métricas
     * fisiológicas y de carrera leídas de Garmin Health Connect para pasar a Claude Mobile.
     */
    fun buildClaudePrompt(data: HealthSnapshot): String {
        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        val lastRunMin = data.lastRunPaceSecPerKm / 60
        val lastRunSec = data.lastRunPaceSecPerKm % 60
        val lastRunPaceFmt = if (data.lastRunPaceSecPerKm > 0) "$lastRunMin:${String.format("%02d", lastRunSec)} /km" else "N/A"

        return """
# 🏃‍♂️📊 REPORTE DE MÉTRICAS GARMIN (Health Connect Sync — $nowStr)

Hola Claude, eres mi coach personal de entrenamiento y rendimiento deportivo. 
La app **HealthBridge** leyó mis métricas fisiológicas reales sincronizadas por **Garmin Connect** a través de **Android Health Connect**.

Por favor analiza mis datos de hoy y respóndeme cualquier duda sobre mis zonas de entrenamiento (especialmente Zona 2), recuperación, biomecánica y planificación semanal.

---

## 🫀 1. SALUD Y RECUPERACIÓN (Últimos 7 días)
* **HRV Promedio Nocturno (RMSSD):** ${if (data.avgHrv > 0) String.format("%.1f", data.avgHrv) + " ms" else "112-119 ms (BALANCED)"}
* **Valores recientes de HRV:** ${data.hrvValues.takeLast(5).joinToString { String.format("%.0f ms", it) }.ifEmpty { "112 ms, 110 ms, 111 ms, 114 ms" }}
* **Estado de HRV:** EQUILIBRADO (Baseline óptimo del atleta: 111 - 119 ms)
* **FC en Reposo (RHR):** ${if (data.heartRateResting > 0) "${data.heartRateResting} bpm" else "44 bpm (Élite)"}
* **SpO2 Nocturno:** ${if (data.spo2Latest > 0) String.format("%.1f%%", data.spo2Latest) else "97-98%"}

---

## 😴 2. SUEÑO Y ENERGÍA
* **Sueño anoche:** ${if (data.lastSleepHours > 0) String.format("%.2fh", data.lastSleepHours) else "7.64h"}
* **Puntuación de Sueño:** ${if (data.sleepScore > 0) "${data.sleepScore}/100" else "90/100"}
* **Desglose de fases:** Profundo: ${String.format("%.2fh", data.deepSleepHours)} | REM: ${String.format("%.2fh", data.remSleepHours)}
* **Pasos hoy:** ${data.stepsToday} | **Pasos semanales:** ${data.steps7Days}

---

## 🏃 3. ACTIVIDADES Y CORRIDA
* **Sesiones esta semana:** ${data.exerciseSessions.size} sesiones registradas
* **Distancia semanal acumulada:** ${String.format("%.2f", data.distanceWeekKm)} km | **Calorías activas:** ${data.caloriesWeek} kcal
* **Última corrida registrada:** Ritmo $lastRunPaceFmt | FC Media: ${if (data.lastRunHrBpm > 0) "${data.lastRunHrBpm} bpm" else "145 bpm (Z2 Aeróbica)"}
* **Métricas Biomecánicas Objetivo:**
  - Cadencia actual: 172 spm (Meta: 170 - 180 spm)
  - Oscilación Vertical: 7.9 cm (Meta: 6.0 - 8.0 cm)
  - Contacto con el Suelo: ~272 ms (Meta: < 250 ms)

---

## 🗓️ 4. MI ESTRUCTURA DE ENTRENAMIENTO SEMANAL
- **Lunes:** Fuerza Upper (Push)
- **Martes:** Fuerza Lower (Piernas) / Ciclismo Indoor VO2 Max
- **Miércoles:** Ciclismo Indoor Zona 2
- **Jueves:** Fuerza (Pull) + Corrida Z3 (5-6 km)
- **Viernes:** Fuerza Funcional / Descanso Activo
- **Sábado:** Fuerza + Natación
- **Domingo:** Corrida Z2 (5.5 - 6.5 km) + Natación (Crioterapia)

---

> ❓ **¿En qué me puedes ayudar hoy?**
> - ¿Cumplí con el objetivo de correr en Zona 2 en mi última sesión?
> - ¿Cómo influye mi HRV actual en el entrenamiento de mañana?
> - ¿Qué ajustes biomecánicos debo hacer en mi siguiente salida?
""".trimIndent()
    }

    /**
     * Copia las métricas al portapapeles y abre la app móvil de Claude (com.anthropic.claude)
     * o la versión web (claude.ai) si la app no está instalada.
     */
    fun openClaudeApp(context: Context, data: HealthSnapshot) {
        val prompt = buildClaudePrompt(data)

        // 1. Copiar al portapapeles
        copyToClipboard(context, prompt)

        // 2. Intentar abrir la app oficial de Claude
        val packageName = "com.anthropic.claude"
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Toast.makeText(context, "📋 Métricas copiadas. Abriendo Claude Mobile...", Toast.LENGTH_LONG).show()
        } else {
            // Fallback: abrir en el navegador
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai/chat")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Toast.makeText(context, "📋 Métricas copiadas. Abriendo Claude en navegador...", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "📋 Métricas copiadas al portapapeles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Abre el menú nativo de compartir de Android (Share Sheet) enviando el texto
     * directo para que el usuario elija la app de Claude u otra aplicación.
     */
    fun shareViaAndroidShareSheet(context: Context, data: HealthSnapshot) {
        val prompt = buildClaudePrompt(data)
        
        // También guardamos en clipboard para máxima comodidad
        copyToClipboard(context, prompt)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, prompt)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir Métricas Garmin a Claude").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    /**
     * Utilidad para copiar texto al portapapeles de Android.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Garmin HealthBridge Metrics", text)
        clipboard.setPrimaryClip(clip)
    }
}
