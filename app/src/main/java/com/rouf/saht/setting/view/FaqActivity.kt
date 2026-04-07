package com.rouf.saht.setting.view

import android.content.res.Configuration
import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.databinding.ActivityFaqBinding

class FaqActivity : BaseActivity() {

    private lateinit var binding: ActivityFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySurfaceStatusBar()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvFaq.layoutManager = LinearLayoutManager(this)
        binding.rvFaq.adapter = FaqAdapter(buildFaqItems())
    }

    private fun buildFaqItems(): List<FaqItem> = listOf(
        // ── Permissions ──
        FaqItem.Section("Permissions"),

        FaqItem.Entry(
            "Why does the app need camera permission?",
            "The heart rate monitor uses your device's rear camera and flash to detect blood flow through your fingertip. By placing your finger over the camera lens, the app captures subtle colour changes caused by each heartbeat and calculates your pulse. No photos or videos are ever taken or stored."
        ),
        FaqItem.Entry(
            "Why does the app need activity recognition permission?",
            "The pedometer relies on your device's built-in step counter sensor. Android requires the Activity Recognition permission so the app can read step data from that sensor. This permission is used solely for counting steps and is never used to track your location."
        ),
        FaqItem.Entry(
            "Why does the app show a persistent notification?",
            "When the pedometer is running, Android requires a persistent foreground notification to keep the step-counting service alive in the background. Tapping the notification opens the app directly to the Pedometer screen. You can customise notification behaviour in your system notification settings."
        ),
        FaqItem.Entry(
            "Why does the app need Accessibility Service access?",
            "The optional double-tap to lock feature uses Android's Accessibility Service to perform the lock action. This service is only active when you enable the feature in Settings > Customization. It does not read, collect, or transmit any screen content, keystrokes, or personal data. It is used solely to perform the device lock action when a double-tap gesture is detected."
        ),

        // ── Data & Privacy ──
        FaqItem.Section("Data & Privacy"),

        FaqItem.Entry(
            "How is my data stored?",
            "All data — personal information, step counts, heart rate readings, and settings — is stored locally on your device using an on-device database. Nothing is sent to any server or third party."
        ),
        FaqItem.Entry(
            "Is my data encrypted?",
            "Your data is stored in the app's private internal storage, which is sandboxed by Android and inaccessible to other apps. On devices with file-based encryption (Android 7+), data at rest is automatically encrypted by the operating system."
        ),
        FaqItem.Entry(
            "Does the app use the internet?",
            "No. Sehat does not require internet access and works entirely offline. There are no analytics, ads, tracking pixels, or telemetry of any kind."
        ),
        FaqItem.Entry(
            "How can I export or back up my data?",
            "Go to Settings > Data > Export data. You will be prompted to choose a location to save a JSON backup file. To restore, use Import data from the same menu."
        ),
        FaqItem.Entry(
            "How do I delete my data?",
            "You can delete individual pedometer or heart rate records from their respective history screens. To remove all app data, go to your device's Settings > Apps > Sehat > Storage > Clear Data, or simply uninstall the app. Since all data is stored locally, uninstalling the app permanently removes everything."
        ),

        // ── Features ──
        FaqItem.Section("Features"),

        FaqItem.Entry(
            "How accurate is the heart rate measurement?",
            "The camera-based method provides an approximate reading suitable for general wellness tracking. Factors like movement, ambient light, and finger pressure can affect the reading. For medical-grade accuracy, please use a certified medical device."
        ),
        FaqItem.Entry(
            "How accurate is the pedometer?",
            "The pedometer uses your phone's hardware step counter sensor. Accuracy depends on your device and where you carry it. For best results, keep the phone in your pocket or a secure holder. You can adjust the sensitivity in Settings > Pedometer Settings."
        ),
        FaqItem.Entry(
            "Can I change the default screen shown on launch?",
            "Yes. Go to Settings > Customization > Default Screen and pick the tab you would like to see when you open the app."
        ),
        FaqItem.Entry(
            "What does the double-tap to lock feature do?",
            "When enabled in Settings > Customization, double-tapping anywhere on the screen locks your device instantly. This uses Android's Accessibility Service to perform the lock action."
        ),
        FaqItem.Entry(
            "Can I delete individual records?",
            "Yes. Open any pedometer or heart rate history entry and tap the Delete button. Deleted records cannot be recovered."
        ),

        // ── Disclosures ──
        FaqItem.Section("Important Disclosures"),

        FaqItem.Entry(
            "Medical disclaimer",
            "Sehat is a personal wellness and fitness tracking tool. It is NOT a medical device and is not intended to diagnose, treat, cure, or prevent any disease or health condition. Heart rate readings and step counts are approximate and for informational purposes only. Always consult a qualified healthcare professional for medical advice. Do not rely on this app for any medical decisions."
        ),
        FaqItem.Entry(
            "Battery usage",
            "The pedometer foreground service and heart rate camera usage may increase battery consumption. The pedometer uses the device's hardware step counter which is designed to be power-efficient. Stopping the pedometer when not needed will help conserve battery life."
        ),
        FaqItem.Entry(
            "Children's privacy",
            "This app is not directed at children under 13. We do not knowingly collect personal information from children. Since all data is stored locally on the device and never transmitted, no personal data is collected by the developer."
        ),
        FaqItem.Entry(
            "Third-party libraries",
            "Sehat uses open-source libraries including MPAndroidChart for data visualisation, Hilt for dependency injection, Room for local database storage, and PaperDB for preferences. These libraries run entirely on-device and do not transmit any data externally."
        ),
        FaqItem.Entry(
            "Contact & support",
            "Sehat is developed and maintained independently. If you have questions, feedback, or encounter any issues, you can reach out through the app's GitHub repository or via the contact information listed on the app's Play Store page."
        ),
    )

    private fun applySurfaceStatusBar() {
        window.statusBarColor = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorSurface, 0
        )
        val isNight = resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isNight
    }
}
