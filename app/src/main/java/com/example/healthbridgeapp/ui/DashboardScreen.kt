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
import com.example.healthbridgeapp.ai.AIInsight

// ── Paleta de colores premium ────────────────────────────────────────────────
private val DarkBg = Color(0xFF0A0E1A)
private val CardBg = Color(0xFF141829)
private val CardBorder = Color(0xFF1E2A45)
private val AccentBlue = Color(0xFF4F8EF7)
private val AccentGreen = Color(0xFF00E5A0)
private val AccentYellow = Color(0xFFFFB740)
private val AccentRed = Color(0xFFFF5252)
private val TextPrimary = Color(0xFFEEF0F8)
private val TextSecondary = Color(0xFF8A92B2)

@Composable
fun DashboardScreen(
    insight: AIInsight?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRefresh: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        text = "Garmin × Gemini",
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
                // Recomendación del día
                RecommendationCard(insight = insight)
                // Métricas clave
                MetricsRow(insight = insight)
                // Proyección semanal
                ProjectionCard(insight = insight)
                // Alerta si existe
                insight.alerta?.let { AlertCard(message = it) }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
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
                    text = "Estado de recuperación hoy",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title = "🎯 Recomendación de hoy")
            Text(
                text = insight.recomendacion_hoy,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            if (insight.listo_para_correr && insight.distancia_recomendada_km > 0) {
                Divider(color = CardBorder, thickness = 1.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RunChip(
                        label = "Distancia",
                        value = "${insight.distancia_recomendada_km} km"
                    )
                    RunChip(
                        label = "Ritmo objetivo",
                        value = insight.ritmo_recomendado
                    )
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
            emoji = "⚡",
            label = "Body Battery",
            value = "${insight.body_battery_estimado}",
            unit = "/100",
            valueColor = when {
                insight.body_battery_estimado >= 70 -> AccentGreen
                insight.body_battery_estimado >= 40 -> AccentYellow
                else -> AccentRed
            }
        )
        MetricTile(
            modifier = Modifier.weight(1f),
            emoji = "🏃",
            label = "¿Corre hoy?",
            value = if (insight.listo_para_correr) "SÍ" else "NO",
            unit = "",
            valueColor = if (insight.listo_para_correr) AccentGreen else AccentRed
        )
    }
}

@Composable
private fun ProjectionCard(insight: AIInsight) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = "📈 Proyección 7 días")
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

// ── Componentes reutilizables ────────────────────────────────────────────────

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
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
    Text(text = title, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp)
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
                        Text(text = unit, color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 2.dp))
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
