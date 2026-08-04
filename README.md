# 🏃‍♂️🧠 Garmin HealthBridge Gemini

[![Android 16](https://img.shields.io/badge/Android-16%2B-green.svg)](https://developer.android.com/about/versions/16)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Health Connect](https://img.shields.io/badge/Health%20Connect-SDK%201.1.0-brightgreen.svg)](https://developer.android.com/guide/health-and-fitness/health-connect)
[![Gemini AI](https://img.shields.io/badge/Gemini%20AI-3.1%20Flash-orange.svg)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Garmin HealthBridge Gemini** is a native Android 16 application that bridges your **Garmin Connect** biometric data (via **Android Health Connect**) directly with **Google Gemini 3.1 AI** for real-time, personalized fitness coaching, biomechanical feedback, and adaptive HRV auto-regulation.

---

## 🌟 Key Features

* **📱 Native Android Health Connect Integration:** Reads nocturnal HRV (RMSSD), Sleep Architecture (Deep, REM, Light), Resting Heart Rate, SpO2, Steps, Active Calories, and Running Pace automatically.
* **⚡ Ultra-Fast 100ms Instant Load Architecture:** Executes parallel Health Connect queries on `Dispatchers.IO` with 2000ms query timeouts, rendering metrics instantly in 100ms while AI enrichment runs asynchronously in the background.
* **🧠 Multi-AI Model Integration (Gemini 2.0 & Claude AI):** Direct HTTPS REST integration with Google Gemini 2.0 Flash and Claude AI for real-time personalized coaching and workout adjustment.
* **🗓️ Adaptive Weekly Training Plan:** Aligns daily AI advice with an adaptive 7-day schedule (Strength, Running, Indoor Cycling, Swimming & Active Recovery).
* **💓 HRV Auto-Regulation Rules:** Dynamically adjusts exercise intensity based on nocturnal HRV baseline:
  * **> 115 ms (🟢 OPTIMAL):** 100% full workload authorized.
  * **105–115 ms (🟡 CAUTION):** Standard plan execution without additional volume.
  * **95–105 ms (🟠 ATTENTION):** Running/cycling load reduced by 30%.
  * **< 95 ms or Low Sleep (🔴 ALERT):** Replaces intense workout with Zone 1 active recovery.
* **🔒 Samsung Knox & Hardware Keystore Resilience:** API keys are secured with AES-256-GCM using hardware-isolated Android Keystore with soft fallback protection against `AEADBadTagException` during reinstalls.
* **🎨 Glassmorphic Premium Dark Mode UI:** Built with Jetpack Compose, status bar padding clearance, and dynamic gradient indicators.

---

## 📊 Biometrics Data Pipeline & Calculations

HealthBridgeApp categorizes telemetry into two distinct pipelines:

| Metric Category | Telemetry / Metric | Source & Processing Method |
| :--- | :--- | :--- |
| **Real Health Connect Data** | Nocturnal HRV (RMSSD), Sleep Hours, Sleep Quality Score, Resting Heart Rate, Steps, SpO2, Calories | Read directly from local Samsung / Android **Health Connect** provider synchronized from Garmin Connect. |
| **Algorithmic Biomechanics** | Cadence (spm), Ground Contact Time (ms), Vertical Oscillation (cm) | Calculated dynamically based on running speed (`lastRunPaceSecPerKm`), as Health Connect API does not store raw 3D running dynamics. |

---

## 📐 Architecture & Data Flow

```
┌─────────────────┐       Auto-Sync       ┌─────────────────────┐
│ Garmin Watch /  │ ────────────────────> │ Android             │
│ Garmin Connect  │                       │ Health Connect DB   │
└─────────────────┘                       └──────────┬──────────┘
                                                     │ Local IO Read (<100ms)
                                                     ▼
┌─────────────────┐     HTTPS REST        ┌─────────────────────┐
│ Google Gemini   │ <───────────────────> │ HealthBridgeApp     │
│ & Claude AI     │  (Encrypted Rest)     │ (Jetpack Compose)   │
└─────────────────┘                       └─────────────────────┘
```

---

## 🛠️ Technology Stack

* **Language:** Kotlin 2.1.0
* **UI Framework:** Jetpack Compose (Material3 Dark Theme + Glassmorphism)
* **Data Layer:** `androidx.health.connect:connect-client:1.1.0`
* **Concurrency:** Kotlin Coroutines (`Dispatchers.IO`, `withTimeoutOrNull(2000)`)
* **Network & Serialization:** Native HTTPS REST (`HttpURLConnection`) + `kotlinx.serialization.json`
* **Security Layer:** `androidx.security:security-crypto:1.1.0-alpha06` (Android Keystore)

---

## 🚀 Getting Started

### Prerequisites
* Android 14+ or compatible Android device with Health Connect.
* Garmin Connect app installed and synced with Health Connect.
* Free **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/apikey) or **Claude API Key**.

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/luiszuniga1990/Garmin_Health_Bridge_Gemini.git
   cd Garmin_Health_Bridge_Gemini
   ```

2. **Build Debug APK using Gradle:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected Android device via ADB:**
   ```bash
   adb install -r -d HealthBridgeApp-debug.apk
   ```

---

## 🔒 Security & Privacy

* **Zero External Server Storage:** Biometric data is processed locally and transmitted directly to Google Gemini / Claude APIs via encrypted HTTPS. No intermediate tracking servers or telemetry collection.
* **Keystore Encrypted Storage:** User API keys are stored in encrypted preferences backed by Android Keystore TEE.
* **Strict Network Security Config:** Enforces HTTPS-only traffic (`cleartextTrafficPermitted="false"`).

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

*Developed by Luis Zúñiga — 2026*
