# 🏃‍♂️🧠 Garmin HealthBridge Gemini

[![Android 16](https://img.shields.io/badge/Android-16%2B-green.svg)](https://developer.android.com/about/versions/16)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Health Connect](https://img.shields.io/badge/Health%20Connect-SDK%201.1.0-brightgreen.svg)](https://developer.android.com/guide/health-and-fitness/health-connect)
[![Gemini AI](https://img.shields.io/badge/Gemini%20AI-3.1%20Flash-orange.svg)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Garmin HealthBridge Gemini** is a native Android 16 application that bridges your **Garmin Connect** biometric data (via **Android Health Connect**) directly with **Google Gemini 3.1 AI** for real-time, personalized fitness coaching, biomechanical feedback, and adaptive HRV auto-regulation.

---

## 🌟 Key Features

* **📱 Native Android 16 Health Connect Integration:** Reads nightly HRV (RMSSD), Sleep Architecture (Deep, REM, Light), Resting Heart Rate, SpO2, Steps, Active Calories, and Running Biomechanics automatically.
* **🧠 Real-Time Gemini AI Coaching:** Uses direct HTTPS REST integration with Google Gemini 3.1 AI to generate structured daily workout recommendations and recovery scores.
* **🗓️ Adaptive Weekly Training Plan:** Aligns daily AI advice with an adaptive 7-day schedule (Strength, Running, Indoor Cycling, Swimming & Active Recovery).
* **💓 HRV Auto-Regulation Rules:** Dynamically adjusts exercise intensity based on nocturnal HRV baseline:
  * **> 115 ms (🟢 OPTIMAL):** 100% full workload authorized.
  * **105–115 ms (🟡 CAUTION):** Standard plan execution without additional volume.
  * **95–105 ms (🟠 ATTENTION):** Running/cycling load reduced by 30%.
  * **< 95 ms or Low Sleep (🔴 ALERT):** Replaces intense workout with Zone 1 active recovery.
* **🔒 Hardware-Backed Security:** API keys are encrypted with AES-256-GCM using Android Keystore inside the device's Trusted Execution Environment (TEE).
* **🎨 Glassmorphic Premium Dark Mode UI:** Built with Jetpack Compose, dynamic status gradients, and pulsing animation states.

---

## 📐 Architecture & Data Flow

```
┌─────────────────┐       Auto-Sync       ┌─────────────────────┐
│ Garmin Watch /  │ ────────────────────> │ Android 16          │
│ Garmin Connect  │                       │ Health Connect DB   │
└─────────────────┘                       └──────────┬──────────┘
                                                     │ Local Read
                                                     ▼
┌─────────────────┐     HTTPS REST        ┌─────────────────────┐
│ Google Gemini   │ <───────────────────> │ HealthBridgeApp     │
│ 3.1 AI Engine   │  (x-goog-api-key)     │ (Jetpack Compose)   │
└─────────────────┘                       └─────────────────────┘
```

---

## 🛠️ Technology Stack

* **Language:** Kotlin 2.1.0
* **UI Framework:** Jetpack Compose (Material3 Dark Theme)
* **Data Layer:** `androidx.health.connect:connect-client:1.1.0`
* **Network & Serialization:** Native HTTPS REST (`HttpURLConnection`) + `kotlinx.serialization.json`
* **Security Layer:** `androidx.security:security-crypto:1.1.0-alpha06` (Android Keystore)

---

## 🚀 Getting Started

### Prerequisites
* Android 16 or compatible Android device with Health Connect.
* Garmin Connect app installed and synced with Health Connect.
* Free **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/apikey).

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
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Security & Privacy

* **Zero External Server Storage:** Biometric data is read locally from Health Connect and transmitted directly to Google Gemini API via encrypted HTTPS. No intermediate telemetry or data harvesting servers.
* **Encrypted Storage:** API keys are stored using `EncryptedSharedPreferences` backed by hardware-isolated Android Keystore.
* **Strict Network Security Config:** Enforces HTTPS-only traffic (`cleartextTrafficPermitted="false"`).

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

*Developed by Luis Zúñiga — 2026*
