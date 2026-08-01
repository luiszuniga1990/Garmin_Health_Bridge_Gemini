# Privacy Policy for Garmin HealthBridge Gemini

**Last updated:** August 1, 2026

## 1. Overview
**Garmin HealthBridge Gemini** ("the Application") is an open-source Android application developed by Luis Zúñiga. This Privacy Policy describes how the Application handles user data, specifically health and biometric metrics accessed through Android Health Connect and Google Gemini AI services.

## 2. Health Connect Data Access
The Application requests read-only permissions from **Android Health Connect** for the following metrics:
* Steps Count
* Heart Rate & Resting Heart Rate
* Sleep Sessions & Sleep Architecture (Deep, REM, Light)
* Heart Rate Variability (RMSSD)
* Exercise Sessions & Biomechanics (Cadence, Distance, Pace)
* Oxygen Saturation (SpO2) and Respiratory Rate

### Data Processing & Storage
* All biometric data read from Health Connect is processed **locally on your device**.
* The Application **does not collect, store, or sell** your personal or biometric information on any external servers or third-party analytics services.

## 3. Google Gemini AI Integration
To provide personalized coaching insights, anonymized biometric summaries are sent directly from your device to the **Google Gemini API** (`https://generativelanguage.googleapis.com`) using encrypted HTTPS protocols.
* Transfers occur solely between your device and Google's official API servers.
* No personal identifying information (such as real name, email, or exact GPS coordinates) is included in API prompts.

## 4. API Key & Security
Your Google Gemini API Key is stored locally on your Android device using **Android Keystore (AES-256-GCM encryption)** via `EncryptedSharedPreferences`. The key never leaves your device except as an authentication header to Google API servers.

## 5. Contact Information
If you have any questions or concerns regarding this Privacy Policy, please open an issue on the official GitHub repository:  
https://github.com/luiszuniga1990/Garmin_Health_Bridge_Gemini
