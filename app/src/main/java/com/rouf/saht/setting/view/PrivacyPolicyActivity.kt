package com.rouf.saht.setting.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : BaseActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.navigationIcon?.setTint(
            androidx.core.content.ContextCompat.getColor(this, R.color.dark_grey)
        )
        binding.toolbar.setNavigationOnClickListener { finish() }

        val surfaceColor = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurface, 0
        )
        binding.appBar.setBackgroundColor(surfaceColor)
        binding.toolbar.setBackgroundColor(surfaceColor)
        window.statusBarColor = surfaceColor

        val isNight = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isNight

        binding.rvPrivacy.layoutManager = LinearLayoutManager(this)
        binding.rvPrivacy.adapter = FaqAdapter(buildPrivacyItems())
    }

    private fun buildPrivacyItems(): List<FaqItem> = listOf(
        // ── Overview ──
        FaqItem.Section("Overview"),

        FaqItem.Entry(
            "Our privacy principle",
            "Sehat is a personal health tracking app built on a single principle: your health data is yours alone. All data stays on your device. Nothing is sent to any server or third party."
        ),
        FaqItem.Entry(
            "Last updated",
            "This privacy policy was last updated in April 2026."
        ),

        // ── Data Collection ──
        FaqItem.Section("Data We Collect"),

        FaqItem.Entry(
            "Step count, distance, calories & duration",
            "Collected by the pedometer feature using your device's built-in step counter sensor. All data is stored on your device only and is never transmitted."
        ),
        FaqItem.Entry(
            "Heart rate (BPM) and activity context",
            "Measured using the heart rate monitor feature via your device's rear camera. Readings are stored on your device only. No images or video are ever recorded, saved, or transmitted."
        ),
        FaqItem.Entry(
            "Personal information",
            "Name, date of birth, gender, height, and weight are used for BMI calculation and calorie estimates. This information is stored on your device only."
        ),
        FaqItem.Entry(
            "Meditation sound URIs",
            "If you select a custom meditation sound, a pointer (URI) to the file on your device is stored locally. The sound file itself is not copied or uploaded."
        ),
        FaqItem.Entry(
            "Breathing exercises",
            "The guided breathing exercises (4-7-8 and Box Breathing) run entirely on-device using simple animations and timers. No data is collected, recorded, or stored during a breathing session."
        ),
        FaqItem.Entry(
            "App settings and preferences",
            "Theme choices, dark mode, pedometer settings, heart rate monitor settings, default screen, navigation tab order, dashboard card visibility, and other preferences are stored locally on your device."
        ),

        // ── What We Don't Do ──
        FaqItem.Section("What Sehat Does NOT Do"),

        FaqItem.Entry(
            "No network access",
            "Sehat does not request the INTERNET permission. The app is technically incapable of making any network connection. It works entirely offline."
        ),
        FaqItem.Entry(
            "No analytics or tracking",
            "Sehat does not collect analytics, usage statistics, crash reports, or any form of telemetry."
        ),
        FaqItem.Entry(
            "No advertisements",
            "Sehat does not display any advertisements and does not integrate any ad network SDKs."
        ),
        FaqItem.Entry(
            "No data sharing",
            "Sehat does not sell, rent, share, or transmit your data to anyone. Your data never leaves your device."
        ),

        // ── Permissions ──
        FaqItem.Section("Permissions Explained"),

        FaqItem.Entry(
            "Camera",
            "Used solely to measure heart rate by analysing light reflected from your fingertip. No images or video are ever recorded, saved, or transmitted. The camera is active only while measuring heart rate."
        ),
        FaqItem.Entry(
            "Activity Recognition",
            "Required by Android to access the device's step counter sensor. Data is processed on-device; only the final step count and derived metrics are stored locally."
        ),
        FaqItem.Entry(
            "Foreground Service / Notifications",
            "Android requires a persistent notification to keep the pedometer step-counting service and meditation audio playback running in the background. These notifications are only shown while their respective features are active."
        ),
        FaqItem.Entry(
            "Audio (READ_MEDIA_AUDIO)",
            "Required only when you add a custom meditation sound from your device storage. Sehat reads the selected audio file for playback; it does not scan, index, or access any other audio files on your device."
        ),
        FaqItem.Entry(
            "Accessibility Service",
            "Used only for the optional double-tap to lock feature. When enabled, it listens for a double-tap gesture and performs a screen lock action. It does not read, collect, or transmit any screen content, keystrokes, or personal data."
        ),

        // ── Data Management ──
        FaqItem.Section("Data Management"),

        FaqItem.Entry(
            "Data storage",
            "All data is stored in the app's private internal storage, which is sandboxed by Android and inaccessible to other apps. On devices with file-based encryption (Android 7+), data at rest is automatically encrypted by the operating system."
        ),
        FaqItem.Entry(
            "Data backup and export",
            "The in-app backup feature lets you export your health data to a JSON file on your own device. You choose the save location. The file is not transmitted anywhere \u2014 it stays on your device until you move or delete it."
        ),
        FaqItem.Entry(
            "Data deletion",
            "You can delete individual records from the history screens. To delete all data stored by Sehat, go to your device Settings > Apps > Sehat > Storage > Clear Data, or simply uninstall the app. Android will remove all app data automatically. If you exported a backup file, delete it manually."
        ),
        FaqItem.Entry(
            "Children's privacy",
            "This app is not directed at children under the age of 13. Since all data is stored locally on the device and never transmitted, no personal data is collected by the developer."
        ),

        // ── Contact ──
        FaqItem.Section("Contact"),

        FaqItem.Entry(
            "Questions or concerns?",
            "If you have any questions about this privacy policy or the app's data practices, please open an issue on the Sehat GitHub repository or reach out via the contact information listed on the app's store page."
        ),
    )

}
