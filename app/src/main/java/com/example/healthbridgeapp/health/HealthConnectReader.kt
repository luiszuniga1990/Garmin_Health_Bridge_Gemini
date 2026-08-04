package com.example.healthbridgeapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * Lee todos los datos relevantes de Health Connect de forma concurrente y ultrarrápida.
 */
class HealthConnectReader(private val client: HealthConnectClient) {

    /**
     * Lee un snapshot completo de los últimos 7 días.
     * Ejecuta todas las consultas en paralelo con async/await.
     */
    suspend fun readLastWeekSnapshot(): HealthSnapshot = coroutineScope {
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)
        val sleepTimeRange = TimeRangeFilter.between(now.minus(36, ChronoUnit.HOURS), now)
        val timeRange7d = TimeRangeFilter.between(sevenDaysAgo, now)
        val timeRange1d = TimeRangeFilter.between(oneDayAgo, now)

        val hrTimeRange = TimeRangeFilter.between(now.minus(6, ChronoUnit.HOURS), now)

        // ── Consultas paralelas en segundo plano (Timeout máximo 2000ms por consulta) ──────────────────────────────
        val hrvDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRange7d)
                    ).records.sortedBy { it.time }
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }

        val sleepDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(SleepSessionRecord::class, sleepTimeRange)
                    ).records.sortedBy { it.startTime }
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }

        val stepsWeekDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange7d)).records.sumOf { it.count }
                } ?: 0L
            } catch (e: Exception) { 0L }
        }

        val stepsTodayDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange1d)).records.sumOf { it.count }
                } ?: 0L
            } catch (e: Exception) { 0L }
        }

        val exerciseDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(ExerciseSessionRecord::class, timeRange7d)
                    ).records.sortedBy { it.startTime }
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }

        val hrDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(ReadRecordsRequest(HeartRateRecord::class, hrTimeRange)).records
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }
        }

        val spo2Deferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(OxygenSaturationRecord::class, timeRange7d)
                    ).records.maxByOrNull { it.time }?.percentage?.value ?: 0.0
                } ?: 0.0
            } catch (e: Exception) { 0.0 }
        }

        val activeCalDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange7d)
                    ).records.sumOf { it.energy.inKilocalories.toLong() }
                } ?: 0L
            } catch (e: Exception) { 0L }
        }

        val totalCalDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRange7d)
                    ).records.sumOf { it.energy.inKilocalories.toLong() }
                } ?: 0L
            } catch (e: Exception) { 0L }
        }

        val distDeferred = async {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    client.readRecords(
                        ReadRecordsRequest(DistanceRecord::class, timeRange7d)
                    ).records.sumOf { it.distance.inKilometers }
                } ?: 0.0
            } catch (e: Exception) { 0.0 }
        }

        // ── Esperar resultados concurrentes ─────────────────────────────────
        val hrvRecords = hrvDeferred.await()
        val sleepRecords = sleepDeferred.await()
        val stepsWeek = stepsWeekDeferred.await()
        val stepsToday = stepsTodayDeferred.await()
        val exerciseRecords = exerciseDeferred.await()
        val hrRecords = hrDeferred.await()
        val spo2 = spo2Deferred.await()
        val activeCalories = activeCalDeferred.await()
        val totalCalories = totalCalDeferred.await()
        val distanceKm = distDeferred.await()

        val hrvValues = hrvRecords.map { it.heartRateVariabilityMillis }
        val avgHrv = if (hrvValues.isNotEmpty()) hrvValues.average() else 0.0

        val lastSleep = sleepRecords.maxByOrNull { it.endTime }
        val lastSleepDuration = lastSleep?.let {
            (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0
        } ?: 0.0

        val deepSleepHours = lastSleep?.stages
            ?.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
            ?.sumOf { (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0 } ?: 0.0

        val remSleepHours = lastSleep?.stages
            ?.filter { it.stage == SleepSessionRecord.STAGE_TYPE_REM }
            ?.sumOf { (it.endTime.epochSecond - it.startTime.epochSecond) / 3600.0 } ?: 0.0

        val sleepScore = calculateSleepScore(lastSleepDuration, deepSleepHours, remSleepHours)

        val exercises = exerciseRecords.takeLast(10).map { ex ->
            ExerciseInfo(
                type = ex.exerciseType.toString(),
                durationMin = (ex.endTime.epochSecond - ex.startTime.epochSecond) / 60.0,
                caloriesBurned = 0L,
                startTime = ex.startTime
            )
        }

        val allHrSamples = hrRecords.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
        val restingHr = allHrSamples.filter { it in 35..100 }.minOrNull() ?: 0

        val calories = if (activeCalories > 0) activeCalories else totalCalories

        val runningTypes = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
        )
        val lastRun = exerciseRecords.lastOrNull { it.exerciseType in runningTypes }
        
        var lastRunPace = 0
        var lastRunHr = 0

        if (lastRun != null) {
            val runDurationMin = (lastRun.endTime.epochSecond - lastRun.startTime.epochSecond) / 60.0

            val runHrSamples = hrRecords
                .filter { it.startTime >= lastRun.startTime.minusSeconds(120) && it.endTime <= lastRun.endTime.plusSeconds(120) }
                .flatMap { it.samples }
                .map { it.beatsPerMinute.toInt() }

            if (runHrSamples.isNotEmpty()) {
                lastRunHr = runHrSamples.average().toInt()
            }

            if (distanceKm > 0.1 && runDurationMin > 0) {
                lastRunPace = ((runDurationMin * 60) / distanceKm).toInt()
            } else if (runDurationMin > 0) {
                lastRunPace = (runDurationMin * 60 / 5.0).toInt()
            }
        }

        HealthSnapshot(
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
