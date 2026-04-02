# Sehat — Personal Health Tracker

Sehat (سیہت, meaning *health* in Urdu) is an Android app for tracking your daily fitness and wellness. It measures steps, heart rate, and supports meditation — all without ever leaving your device.

## Why it exists

Most health apps send your data to the cloud, sell it to advertisers, or require an account just to count your steps. Sehat exists to give you a personal health tracker that stays entirely on your phone. No accounts. No servers. No tracking.

## Privacy first

**Your data never leaves your device.**

- No internet permission is requested — the app cannot make network calls
- All data is stored locally using an embedded on-device database ([PaperDB](https://github.com/pilgr/Paper))
- No analytics, no crash reporting, no ad SDKs
- Camera is used only to measure heart rate — no frames are recorded or stored
- Step and heart rate data is stored only in your device's internal storage

The only time data moves is when *you* explicitly export it via the in-app backup feature, which saves a JSON file to a location you choose on your own device.

## Features

### Pedometer
Counts steps using the device's built-in step counter sensor. Runs as a foreground service so it keeps counting while your phone is in your pocket.

- Real-time step, distance, calorie, and duration tracking
- Configurable daily step goal
- Adjustable sensor sensitivity (Low / Medium / High / Very High)
- Session history with per-session detail view
- BMI display calculated from your personal information

### Heart Rate Monitor
Measures heart rate using the camera. Place your finger over the lens — the app analyzes subtle changes in the red channel to detect your pulse.

- Real-time BPM display with a live chart
- Configurable measurement duration and sensitivity
- Saves measurements with activity context (Walking, Running, Cycling, Resting)
- Session history

### Meditation
Plays ambient nature sounds to help you focus or unwind.

- Six ambient soundscapes: Rain, Water Waves, Lightning, Bird Chirping, Wind Breeze, Forest
- Runs as a foreground service for uninterrupted playback
- Rotating motivational quotes

### Data Backup
Export all your health data to a JSON file and import it back at any time.

- Uses Android's Storage Access Framework — no storage permissions required
- You choose where to save the file
- All data (steps history, heart rate history, personal info, settings) is included

## Permissions

| Permission | Reason |
|---|---|
| `ACTIVITY_RECOGNITION` | Step counting |
| `CAMERA` | Heart rate measurement |
| `POST_NOTIFICATIONS` | Foreground service notification |
| `FOREGROUND_SERVICE` | Background step tracking and meditation audio |

## Tech stack

- Kotlin, Android SDK (min API 24, target API 35)
- Dagger Hilt for dependency injection
- PaperDB for local storage
- ExoPlayer for audio playback
- CameraX for heart rate analysis
- Kotlin Coroutines + StateFlow

## Building

```bash
./gradlew assembleDebug
```

Requires Android Studio Hedgehog or later.
