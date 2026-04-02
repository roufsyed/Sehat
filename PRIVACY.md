# Privacy Policy — Sehat

*Last updated: April 2026*

Sehat (سیہت) is a personal health tracking app built on a single principle: **your health data is yours alone**. This policy explains what data the app collects, where it is stored, and what it does not do.

---

## What data Sehat collects

| Data | Purpose | Where it is stored |
|---|---|---|
| Step count, distance, calories, session duration | Pedometer feature | On your device only |
| Heart rate (BPM) and activity context | Heart rate monitor feature | On your device only |
| Personal information (name, age, height, weight, gender) | BMI calculation and calorie estimates | On your device only |
| Custom meditation sounds you add | Meditation feature | File URI on your device; Sehat stores a pointer, not a copy |
| App settings and preferences | App configuration | On your device only |

All data is stored locally using an embedded on-device database (PaperDB). There is no user account, no login, and no server.

---

## What Sehat does NOT do

- Does not transmit health data to any server, cloud service, or third party
- Does not require or use an internet connection for core features
- Does not collect analytics or usage statistics
- Does not display advertisements
- Does not include any third-party advertising or tracking SDKs
- Does not sell, rent, or share your data with anyone

---

## Camera

The camera is used solely to measure heart rate by analyzing subtle changes in light reflected from your fingertip. **No images or video are ever recorded, saved, or transmitted.** The camera is active only while you are actively measuring your heart rate, and stops immediately when you press Stop or the session ends.

---

## Step sensor

The app uses the device's built-in step counter hardware sensor via the `ACTIVITY_RECOGNITION` permission. The sensor data is processed on-device and only the final step count and derived metrics are stored locally.

---

## Network access

Sehat does not request the `INTERNET` permission. The app is technically incapable of making any network connection. No data is ever transmitted from your device.

---

## Data backup and export

The in-app backup feature lets you export all your health data to a JSON file on your own device. You choose the location using Android's standard file picker. The exported file is not transmitted anywhere — it stays on your device until you move or delete it yourself.

---

## Data deletion

To delete all health data stored by Sehat, uninstall the app. Android will remove all app data automatically. If you have exported a backup file, delete that file manually.

---

## Children

Sehat is not directed at children under 13. If you are under 13, please do not use this app.

---

## Changes to this policy

If this policy changes materially, the updated version will be published in this repository with a new "Last updated" date.

---

## Contact

If you have questions about this privacy policy, open an issue in the [Sehat GitHub repository](https://github.com/roufsyed/sehat).
