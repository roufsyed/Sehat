package com.rouf.saht.meditation

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.databinding.ActivityBreathingExerciseBinding

class BreathingExerciseActivity : BaseActivity() {

    private lateinit var binding: ActivityBreathingExerciseBinding

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var currentPhaseIndex = 0
    private var cycleCount = 0

    private lateinit var phases: List<Pair<String, Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingExerciseBinding.inflate(layoutInflater)
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

        val patternType = intent.getStringExtra(EXTRA_PATTERN) ?: PATTERN_BOX
        when (patternType) {
            PATTERN_478 -> {
                supportActionBar?.title = "4-7-8 Breathing"
                phases = listOf("Inhale" to 4, "Hold" to 7, "Exhale" to 8)
                binding.tvPatternInfo.text = "Inhale 4s \u2022 Hold 7s \u2022 Exhale 8s"
            }
            else -> {
                supportActionBar?.title = "Box Breathing"
                phases = listOf("Inhale" to 4, "Hold" to 4, "Exhale" to 4, "Hold" to 4)
                binding.tvPatternInfo.text = "Inhale 4s \u2022 Hold 4s \u2022 Exhale 4s \u2022 Hold 4s"
            }
        }

        // Tint the breathing circles with primary color
        val primary = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        binding.breathingCircle.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 60))
        binding.breathingCircleGlow.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 25))

        binding.tvPhase.text = "Ready"
        binding.tvSeconds.text = ""
        binding.tvCycles.visibility = View.INVISIBLE

        binding.btnStartStop.setOnClickListener {
            if (isRunning) stop() else start()
        }
    }

    private fun start() {
        isRunning = true
        currentPhaseIndex = 0
        cycleCount = 0
        binding.btnStartStop.text = getString(R.string.stop)
        binding.tvCycles.visibility = View.VISIBLE
        binding.tvCycles.text = "Cycle 1"
        runPhase()
    }

    private fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        binding.breathingCircle.animate().cancel()
        binding.breathingCircleGlow.animate().cancel()
        binding.btnStartStop.text = getString(R.string.start)
        binding.tvPhase.text = "Ready"
        binding.tvSeconds.text = ""
        binding.tvCycles.visibility = View.INVISIBLE

        binding.breathingCircle.animate().scaleX(MIN_SCALE).scaleY(MIN_SCALE)
            .setDuration(400).start()
        binding.breathingCircleGlow.animate().scaleX(MIN_SCALE).scaleY(MIN_SCALE)
            .setDuration(400).start()
    }

    private fun runPhase() {
        if (!isRunning) return

        val (phaseName, durationSec) = phases[currentPhaseIndex]
        val durationMs = durationSec * 1000L

        binding.tvPhase.text = phaseName
        binding.tvSeconds.text = durationSec.toString()

        // Animate the circle scale
        val targetScale = when (phaseName) {
            "Inhale" -> MAX_SCALE
            "Exhale" -> MIN_SCALE
            else -> binding.breathingCircle.scaleX
        }
        binding.breathingCircle.animate()
            .scaleX(targetScale).scaleY(targetScale)
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        binding.breathingCircleGlow.animate()
            .scaleX(targetScale).scaleY(targetScale)
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Countdown text via handler
        for (i in 1 until durationSec) {
            val remaining = durationSec - i
            handler.postDelayed({ binding.tvSeconds.text = remaining.toString() }, i * 1000L)
        }

        // Schedule next phase
        handler.postDelayed({
            if (!isRunning) return@postDelayed
            currentPhaseIndex++
            if (currentPhaseIndex >= phases.size) {
                currentPhaseIndex = 0
                cycleCount++
                binding.tvCycles.text = "Cycle ${cycleCount + 1}"
            }
            runPhase()
        }, durationMs)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PATTERN = "breathing_pattern"
        const val PATTERN_478 = "478"
        const val PATTERN_BOX = "box"

        private const val MIN_SCALE = 0.3f
        private const val MAX_SCALE = 1.0f
    }
}
