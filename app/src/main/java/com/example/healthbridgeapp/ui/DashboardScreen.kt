package com.example.healthbridgeapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.healthbridgeapp.ai.AIInsight
import com.example.healthbridgeapp.ai.ClaudeExporter
import com.example.healthbridgeapp.health.HealthSnapshot
import com.example.healthbridgeapp.security.AiProvider

// ── Paleta de colores premium ────────────────────────────────────────────────
private val DarkBg = Color(0xFF0A0E1A)
private val CardBg = Color(0xFF141829)
private val CardBorder = Color(0xFF1E2A45)
private val AccentBlue = Color(0xFF4F8EF7)
private val AccentOrange = Color(0xFFD97706)
private val AccentClaude = Color(0xFFDA7756)
private val AccentGreen = Color(0xFF00E5A0)
private val AccentYellow = Color(0xFFFFB740)
private val AccentRed = Color(0xFFFF5252)
private val TextPrimary = Color(0xFFEEF0F8)
private val TextSecondary = Color(0xFF8A92B2)

@Composable
fun DashboardScreen(
    insight: AIInsight?,
    snapshot: HealthSnapshot? = null,
    isLoading: Boolean,
    errorMessage: String? = null,
    selectedProvider: AiProvider = AiProvider.GEMINI,
    onRefresh: () -> Unit,
    onProviderSwitched: (AiProvider) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HealthBridge",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Garmin Connect × Claude AI × Gemini",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CardBg)
                        .size(44.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isLoading) 360f else 0f,
                        animationSpec = if (isLoading)
                            infiniteRepeatable(tween(1000, easing = LinearEasing))
                        else tween(0),
                        label = "refresh_rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = AccentBlue,
                        modifier = Modifier.scale(if (isLoading) 1.1f else 1f)
                    )
                }
            }

            // ── Selector de Proveedor de IA ───────────────────────────────
            ProviderSelectorRow(
                selectedProvider = selectedProvider,
                onProviderSwitched = onProviderSwitched
            )

            // ── Tarjeta de Exportación a Claude Mobile App ────────────────
            ClaudeExportCard(
                snapshot = snapshot,
                onOpenClaude = {
                    val targetSnapshot = snapshot ?: HealthSnapshot(
                        hrvValues = listOf(112.0, 114.0, 119.0, 110.0, 111.0),
                        avgHrv = 112.0,
                        lastSleepHours = 7.64,
                        sleepScore = 90,
                        deepSleepHours = 0.97,
                        remSleepHours = 1.63,
                        steps7Days = 56641,
                        stepsToday = 8127,
                        exerciseSessions = emptyList(),
                        lastRunPaceSecPerKm = 387,
                        lastRunHrBpm = 145,
                        spo2Latest = 98.0,
                        heartRateResting = 44,
                        caloriesWeek = 3750,
                        distanceWeekKm = 10.73
                    )
                    ClaudeExporter.openClaudeApp(context, targetSnapshot)
                },
                onShareSheet = {
                    val targetSnapshot = snapshot ?: HealthSnapshot(
                        hrvValues = listOf(112.0, 114.0, 119.0, 110.0, 111.0),
                        avgHrv = 112.0,
                        lastSleepHours = 7.64,
                        sleepScore = 90,
                        deepSleepHours = 0.97,
                        remSleepHours = 1.63,
                        steps7Days = 56641,
                        stepsToday = 8127,
                        exerciseSessions = emptyList(),
                        lastRunPaceSecPerKm = 387,
                        lastRunHrBpm = 145,
                        spo2Latest = 98.0,
                        heartRateResting = 44,
                        caloriesWeek = 3750,
                        distanceWeekKm = 10.73
                    )
                    ClaudeExporter.shareViaAndroidShareSheet(context, targetSnapshot)
                }
            )

            // ── Loading state ────────────────────────────────────────────────
            if (isLoading) {
                LoadingCard()
                return@Column
            }

            // ── Error state ──────────────────────────────────────────────────
            if (errorMessage != null) {
                ErrorCard(message = errorMessage, onRetry = onRefresh)
                return@Column
            }

            // ── Insight content ──────────────────────────────────────────────
            if (insight != null) {
                // Estado principal
                StatusCard(insight = insight)

                // 🗓️ Tarjeta de Calendario Semanal de Entrenamiento
                WeeklyPlanCard(insight = insight)

                // 🎯 Recomendación del día
                RecommendationCard(insight = insight)

                // 🏃‍♂️ Biomecánica y VO2 Max
                BiomechanicsCard(insight = insight)

                // ⚡ Métricas clave
                MetricsRow(insight = insight)

                // 📈 Proyección semanal
                ProjectionCard(insight = insight)

                // Alerta si existe
                insight.alerta?.let { AlertCard(message = it) }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProviderSelectorRow(
    selectedProvider: AiProvider,
    onProviderSwitched: (AiProvider) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(14.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val geminiSelected = selectedProvider == AiProvider.GEMINI
        val claudeSelected = selectedProvider == AiProvider.CLAUDE

        Button(
            onClick = { onProviderSwitched(AiProvider.GEMINI) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (geminiSelected) AccentBlue else Color.Transparent,
                contentColor = if (geminiSelected) Color.White else TextSecondary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "✨ Gemini 3.1",
                fontSize = 13.sp,
                fontWeight = if (geminiSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Button(
            onClick = { onProviderSwitched(AiProvider.CLAUDE) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (claudeSelected) AccentClaude else Color.Transparent,
                contentColor = if (claudeSelected) Color.White else TextSecondary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "🟧 Claude AI",
                fontSize = 13.sp,
                fontWeight = if (claudeSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ClaudeExportCard(
    snapshot: HealthSnapshot?,
    onOpenClaude: () -> Unit,
    onShareSheet: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E1911), Color(0xFF141829))
                    )
                )
                .border(1.dp, Color(0xFF5A3121), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "📲", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "Conversar en Claude App Mobile",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transfiere tus métricas de Garmin a la app oficial de Claude",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenClaude,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentClaude,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "💬 Abrir en Claude App",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onShareSheet,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, Color(0xFF7A4533)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "📤 Compartir",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(insight: AIInsight) {
    val statusColor = when (insight.estado) {
        "ÓPTIMO" -> AccentGreen
        "PRECAUCIÓN" -> AccentYellow
        "ALARMA" -> AccentRed
        else -> TextSecondary
    }
    val gradientColors = when (insight.estado) {
        "ÓPTIMO" -> listOf(Color(0xFF0D2B1F), Color(0xFF141829))
        "PRECAUCIÓN" -> listOf(Color(0xFF2B1F0D), Color(0xFF141829))
        "ALARMA" -> listOf(Color(0xFF2B0D0D), Color(0xFF141829))
        else -> listOf(CardBg, CardBg)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = insight.emoji_estado,
                    fontSize = 52.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = insight.estado,
                    color = statusColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estado de recuperación del sistema nervioso",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WeeklyPlanCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "🗓️ SEMANA DE ENTRENAMIENTO")
                Text(
                    text = "Plan 360",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val days = listOf(
                "Lun" to "💪 Fuerza",
                "Mar" to "💪 Fuerza",
                "Mié" to "🚴 Ciclismo Z2",
                "Jue" to "💪 Fuerza + 🏃",
                "Vie" to "💪 Fuerza/Desc",
                "Sáb" to "💪 Fuerza + 🏊",
                "Dom" to "🏃 Corrida + 🏊"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { (day, activity) ->
                    val isTodayActivity = insight.actividad_hoy_nombre.contains(activity.take(6), ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTodayActivity) AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = day,
                            color = if (isTodayActivity) AccentBlue else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activity.take(2),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Programado hoy:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = insight.actividad_hoy_nombre,
                    color = AccentGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BiomechanicsCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "🏃‍♂️ BIOMECÁNICA & RENDIMIENTO")
                Text(
                    text = "VO₂ Max: ${insight.vo2_max_estimado}",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            BiomechanicRow(
                label = "Cadencia",
                value = "${insight.cadencia_actual_spm} spm",
                target = "Meta: 170-175",
                isGood = insight.cadencia_actual_spm >= 170
            )
            BiomechanicRow(
                label = "Contacto Suelo",
                value = "${insight.contacto_suelo_ms} ms",
                target = "Meta: <250 ms",
                isGood = insight.contacto_suelo_ms <= 250
            )
            BiomechanicRow(
                label = "Oscilación Vert.",
                value = "${insight.oscilacion_vertical_cm} cm",
                target = "Meta: 6-8 cm",
                isGood = insight.oscilacion_vertical_cm <= 8.0
            )
        }
    }
}

@Composable
private fun BiomechanicRow(label: String, value: String, target: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = target, color = TextSecondary, fontSize = 11.sp)
        }
        Text(
            text = value,
            color = if (isGood) AccentGreen else AccentYellow,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecommendationCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(title = "🎯 RECOMENDACIÓN Y AJUSTE DE HOY")
                
                val adjustmentColor = when {
                    insight.ajuste_entrenamiento_hoy.contains("100%", ignoreCase = true) -> AccentGreen
                    insight.ajuste_entrenamiento_hoy.contains("Reducir", ignoreCase = true) -> AccentYellow
                    else -> AccentRed
                }
                
                Surface(
                    color = adjustmentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, adjustmentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = insight.ajuste_entrenamiento_hoy,
                        color = adjustmentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = insight.recomendacion_hoy,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            if (insight.distancia_recomendada_km > 0) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RunChip(
                        label = "Distancia",
                        value = "${insight.distancia_recomendada_km} km"
                    )
                    if (insight.ritmo_recomendado.isNotEmpty()) {
                        RunChip(
                            label = "Ritmo objetivo",
                            value = insight.ritmo_recomendado
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(insight: AIInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricTile(
            modifier = Modifier.weight(1f),
            emoji = "😴",
            label = "Estado de Sueño",
            value = insight.estado_sueño_descanso,
            unit = "",
            valueColor = when (insight.estado_sueño_descanso.uppercase()) {
                "ÓPTIMO" -> AccentGreen
                "PRECAUCIÓN" -> AccentYellow
                else -> AccentRed
            }
        )
        MetricTile(
            modifier = Modifier.weight(1f),
            emoji = "🛡️",
            label = "Sobreentrenamiento",
            value = "Riesgo ${insight.riesgo_sobreentrenamiento}",
            unit = "",
            valueColor = when (insight.riesgo_sobreentrenamiento.uppercase()) {
                "BAJO" -> AccentGreen
                "MODERADO" -> AccentYellow
                else -> AccentRed
            }
        )
    }
}

@Composable
private fun ProjectionCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = "📈 PROYECCIÓN 7 DÍAS")
            Text(
                text = insight.proyeccion_semana,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🔑 Métrica clave: ${insight.metrica_clave}",
                color = AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AlertCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1A0D))
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, AccentYellow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚠️", fontSize = 24.sp)
            Text(text = message, color = AccentYellow, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun LoadingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    GlassCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "🧠", fontSize = 48.sp, modifier = Modifier.scale(alpha))
            Text(
                text = "Leyendo Health Connect\ny analizando con Gemini...",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    GlassCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "❌", fontSize = 40.sp)
            Text(text = message, color = AccentRed, fontSize = 14.sp, textAlign = TextAlign.Center)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Reintentar", color = Color.White)
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Box(
            modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = AccentBlue,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Box(modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(16.dp))) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = emoji, fontSize = 28.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (unit.isNotEmpty()) {
                        Text(text = unit, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
                Text(text = label, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RunChip(label: String, value: String) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = AccentGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
