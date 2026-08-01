package com.example.healthbridgeapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Snapshot de todas las métricas de salud leídas de Health Connect.
 * Mapea directamente los datos que Garmin Connect sube a Health Connect.
 */
data class HealthSnapshot(
    val hrvValues: List<Double>,          // ms RMSSD nocturnos
    val avgHrv: Double,                   // promedio HRV 7 días
    val lastSleepHours: Double,           // horas de sueño anoche
    val sleepScore: Int,                  // aproximado de calidad 0-100
    val deepSleepHours: Double,           // horas de sueño profundo
    val remSleepHours: Double,            // horas REM
    val steps7Days: Long,                 // pasos últimos 7 días
    val stepsToday: Long,                 // pasos hoy
    val exerciseSessions: List<ExerciseInfo>, // actividades recientes
    val lastRunPaceSecPerKm: Int,         // pace en segundos/km
    val lastRunHrBpm: Int,                // FC media última corrida
    val spo2Latest: Double,               // SpO2 más reciente
    val heartRateResting: Int,            // FC reposo estimada
    val caloriesWeek: Long,               // calorías activas semana
    val distanceWeekKm: Double,           // km corridos semana
)

data class ExerciseInfo(
    val type: String,
    val durationMin: Double,
    val caloriesBurned: Long,
    val startTime: Instant,
)

/**
 * Lee todos los datos relevantes de Health Connect.
 * En Android 16, Health Connect está integrado nativamente en el OS.
 * Garmin Connect sincroniza automáticamente todos estos datos.
 */
class HealthConnectReader(private val client: HealthConnectClient) {

    /**
     * Lee un snapshot completo de los últimos 7 días.
     * Llamar desde una coroutine (suspend function).
     */
    suspend fun readLastWeekSnapshot(): HealthSnapshot {
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)
        val timeRange7d = TimeRangeFilter.between(sevenDaysAgo, now)
        val timeRange1d = TimeRangeFilter.between(oneDayAgo, now)

        // ── HRV (RMSSD nocturno) ─────────────────────────────────────────
        val hrvRecords = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = timeRange7d
                )
            ).records
        } catch (e: Exception) { emptyList() }

        val hrvValues = hrvRecords.map { it.heartRateVariabilityMillis }
        val avgHrv = if (hrvValues.isNotEmpty()) hrvValues.average() else 0.0

        // ── Sueño ────────────────────────────────────────────────────────
        val sleepRecords = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeRange1d
                )
            ).records
        } catch (e: Exception) { emptyList() }

        val lastSleep = sleepRecords.lastOrNull()
        val lastSleepDuration = lastSleep?.let {
            (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0
        } ?: 0.0

        // Calcular fases de sueño (disponibles en HC desde Android 14)
        val deepSleepHours = lastSleep?.stages
            ?.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
            ?.sumOf { (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0 } ?: 0.0

        val remSleepHours = lastSleep?.stages
            ?.filter { it.stage == SleepSessionRecord.STAGE_TYPE_REM }
            ?.sumOf { (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0 } ?: 0.0

        // Score aproximado: combinación de horas, profundo y REM
        val sleepScore = calculateSleepScore(lastSleepDuration, deepSleepHours, remSleepHours)

        // ── Pasos ─────────────────────────────────────────────────────────
        val stepsWeek = try {
            client.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRange7d)
            ).records.sumOf { it.count }
        } catch (e: Exception) { 0L }

        val stepsToday = try {
            client.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRange1d)
            ).records.sumOf { it.count }
        } catch (e: Exception) { 0L }

        // ── Actividades / Ejercicio ───────────────────────────────────────
        val exerciseRecords = try {
            client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, timeRange7d)
            ).records.takeLast(10)
        } catch (e: Exception) { emptyList() }

        val exercises = exerciseRecords.map { ex ->
            ExerciseInfo(
                type = ex.exerciseType.toString(),
                durationMin = (ex.endTime.epochSecond - ex.startTime.epochSecond) / 60.0,
                caloriesBurned = 0L, // se calcula por separado
                startTime = ex.startTime
            )
        }

        // ── FC (Heart Rate) ───────────────────────────────────────────────
        val hrRecords = try {
            client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRange1d)
            ).records
        } catch (e: Exception) { emptyList() }

        val restingHr = hrRecords
            .flatMap { it.samples }
            .minOfOrNull { it.beatsPerMinute }?.toInt() ?: 0

        // ── SpO2 ──────────────────────────────────────────────────────────
        val spo2 = try {
            client.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, timeRange7d)
            ).records.lastOrNull()?.percentage?.value ?: 0.0
        } catch (e: Exception) { 0.0 }

        // ── Calorías y Distancia ──────────────────────────────────────────
        val calories = try {
            client.readRecords(
                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange7d)
            ).records.sumOf { it.energy.inKilocalories.toLong() }
        } catch (e: Exception) { 0L }

        val distanceKm = try {
            client.readRecords(
                ReadRecordsRequest(DistanceRecord::class, timeRange7d)
            ).records.sumOf { it.distance.inKilometers }
        } catch (e: Exception) { 0.0 }

        // ── Última corrida ─────────────────────────────────────────────────
        val lastRun = exerciseRecords.lastOrNull {
            it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        }
        val lastRunDuration = lastRun?.let {
            (it.endTime.epochSecond - it.startTime.epochSecond) / 60.0
        } ?: 0.0

        // Pace aproximado (sin ruta GPS en HC, estimamos por duración y distancia promedio)
        val lastRunPace = if (lastRunDuration > 0) (lastRunDuration * 60 / 5.0).toInt() else 360

        val lastRunHr = hrRecords.flatMap { it.samples }
            .map { it.beatsPerMinute.toInt() }
            .let { samples -> if (samples.isNotEmpty()) samples.average().toInt() else 0 }

        return HealthSnapshot(
            hrvValues = hrvValues,
            avgHrv = avgHrv,
            lastSleepHours = lastSleepDuration,
            sleepScore = sleepScore,
            deepSleepHours = deepSleepHours,
            remSleepHours = remSleepHours,
            steps7Days = stepsWeek,
            stepsToday = stepsToday,
            exerciseSessions = exercises,
            lastRunPaceSecPerKm = lastRunPace,
            lastRunHrBpm = lastRunHr,
            spo2Latest = spo2,
            heartRateResting = restingHr,
            caloriesWeek = calories,
            distanceWeekKm = distanceKm,
        )
    }

    /**
     * Calcula un score de sueño aproximado (0-100) basado en las métricas disponibles.
     * Garmin tiene su propio algoritmo; aquí aproximamos con HC data.
     */
    private fun calculateSleepScore(totalHours: Double, deepHours: Double, remHours: Double): Int {
        var score = 0
        // Horas totales (máx 40 pts): ideal 7-9h
        score += when {
            totalHours >= 7.0 && totalHours <= 9.0 -> 40
            totalHours >= 6.0 -> 30
            totalHours >= 5.0 -> 20
            else -> 10
        }
        // Sueño profundo (máx 35 pts): ideal > 1h
        score += when {
            deepHours >= 1.5 -> 35
            deepHours >= 1.0 -> 28
            deepHours >= 0.5 -> 18
            else -> 8
        }
        // REM (máx 25 pts): ideal > 1.5h
        score += when {
            remHours >= 1.5 -> 25
            remHours >= 1.0 -> 18
            remHours >= 0.5 -> 10
            else -> 5
        }
        return score.coerceIn(0, 100)
    }
}
