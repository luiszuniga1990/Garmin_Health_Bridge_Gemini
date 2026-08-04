package com.example.healthbridgeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF0A0E1A)
private val CardBg = Color(0xFF141829)
private val CardBorder = Color(0xFF1E2A45)
private val AccentBlue = Color(0xFF4F8EF7)
private val TextPrimary = Color(0xFFEEF0F8)
private val TextSecondary = Color(0xFF8A92B2)

/**
 * Pantalla de onboarding: solicita la Gemini API key al usuario.
 * Se muestra solo la primera vez. La key se guarda cifrada en Keystore.
 */
@Composable
fun OnboardingScreen(onApiKeySubmitted: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo / hero
            Text(text = "🏃‍♂️🧠", fontSize = 64.sp)
            Text(
                text = "HealthBridge",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Garmin × Health Connect × Gemini",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card de instrucciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Box(modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(20.dp))) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🔑 Configura tu Gemini API Key",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "1. Ve a aistudio.google.com/apikey\n" +
                                    "2. Crea una API key gratuita\n" +
                                    "3. Pégala aquí — se guarda cifrada en tu dispositivo",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Input de API key
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    isError = false
                },
                label = { Text("Gemini API Key", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showKey = !showKey }) {
                        Text(
                            text = if (showKey) "Ocultar" else "Ver",
                            color = AccentBlue,
                            fontSize = 12.sp
                        )
                    }
                },
                isError = isError,
                supportingText = if (isError) {
                    { Text("Formato de API key no válido. Verifica tu clave de AI Studio.", color = Color(0xFFFF5252)) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Botón de confirmación
            Button(
                onClick = {
                    if (apiKey.trim().length >= 10) {
                        onApiKeySubmitted(apiKey.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                enabled = apiKey.isNotBlank()
            ) {
                Text(
                    text = "Conectar con Gemini →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Nota de privacidad
            Text(
                text = "🔒 Tu API key se cifra con AES-256 en el Keystore de Android.\nNingún dato sale de tu teléfono excepto hacia la API de Gemini.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
