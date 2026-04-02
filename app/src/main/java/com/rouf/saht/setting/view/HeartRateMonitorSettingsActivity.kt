package com.rouf.saht.setting.view

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rouf.saht.R
import com.rouf.saht.common.DurationConstants
import com.rouf.saht.common.model.HeartRateMonitorSensitivity
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.databinding.ActivityHeartRateMonitorSettingsBinding
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class HeartRateMonitorSettingsActivity : AppCompatActivity() {

    private val TAG: String = HeartRateMonitorSettingsActivity::class.java.simpleName
    private lateinit var binding: ActivityHeartRateMonitorSettingsBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var heartRateMonitorSettings: HeartRateMonitorSettings

    private var oldDuration by Delegates.notNull<Int>()
    private lateinit var oldHeartRateMonitorSensitivity : HeartRateMonitorSensitivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeartRateMonitorSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsViewModel = ViewModelProvider(this@HeartRateMonitorSettingsActivity)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            heartRateMonitorSettings = settingsViewModel.getHeartMonitorSettings()

            oldDuration = heartRateMonitorSettings.duration
            oldHeartRateMonitorSensitivity = heartRateMonitorSettings.sensitivityLevel

            initViews()
            onClick()
        }
    }

    private fun initViews() {
        val duration: Int = heartRateMonitorSettings.duration
        val heartRateMonitorSensitivity: HeartRateMonitorSensitivity = heartRateMonitorSettings.sensitivityLevel

        binding.etDuration.setText(duration.toString())
        Log.d(TAG, "initViews: heartRateMonitorSensitivity -> $heartRateMonitorSensitivity")
        when(heartRateMonitorSensitivity.value) {
            2 -> binding.seekBar.progress = 0
            3 -> binding.seekBar.progress = 1
            5 -> binding.seekBar.progress = 2
        }
    }

    private fun onClick() {

        binding.chip20sec.setOnClickListener {
            binding.etDuration.setText(DurationConstants.DURATION_20_SECONDS.toString())
        }

        binding.chip30sec.setOnClickListener {
            binding.etDuration.setText(DurationConstants.DURATION_30_SECONDS.toString())
        }

        binding.chip45sec.setOnClickListener {
            binding.etDuration.setText(DurationConstants.DURATION_45_SECONDS.toString())
        }

        binding.chip60sec.setOnClickListener {
            binding.etDuration.setText(DurationConstants.DURATION_60_SECONDS.toString())
        }

        binding.etDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val durationText = charSequence.toString().trim()

                if (durationText.isNotEmpty()) {
                    val duration = durationText.toIntOrNull()
                    if (duration != null && duration > 0) {
                        if (duration != oldDuration) {
                            binding.btnSave.isEnabled = true
                        } else {
                            binding.btnSave.isEnabled = false
                        }
                    } else {
                        binding.etDuration.error = "Please enter a valid duration"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    binding.etDuration.error = "duration cannot be empty"
                    binding.btnSave.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        binding.btnSave.setOnClickListener {
            val duration = binding.etDuration.text.toString().trim()
            val heartRateSeekbarProgress = binding.seekBar.progress
            var heartRateMonitorSensitivity: HeartRateMonitorSensitivity = oldHeartRateMonitorSensitivity

            when(heartRateSeekbarProgress) {
                0 -> heartRateMonitorSensitivity = HeartRateMonitorSensitivity.LOW
                1 -> heartRateMonitorSensitivity = HeartRateMonitorSensitivity.MEDIUM
                2 -> heartRateMonitorSensitivity = HeartRateMonitorSensitivity.HIGH
            }

            Log.d(TAG, "duration: $duration, heartRateSeekbarProgress: $heartRateSeekbarProgress, heartRateMonitorSensitivity: ${heartRateMonitorSensitivity.name}")

            heartRateMonitorSettings.duration = duration.toInt()
            heartRateMonitorSettings.sensitivityLevel = heartRateMonitorSensitivity

            lifecycleScope.launch {
                settingsViewModel.saveHeartMonitorSettings(heartRateMonitorSettings)
            }
            binding.btnSave.isEnabled = false

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.Main) {
                showSuccessAnimation()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var newHeartRateMonitorSensitivity: HeartRateMonitorSensitivity = oldHeartRateMonitorSensitivity

                when(progress) {
                    0 -> newHeartRateMonitorSensitivity = HeartRateMonitorSensitivity.LOW
                    1 -> newHeartRateMonitorSensitivity = HeartRateMonitorSensitivity.MEDIUM
                    2 -> newHeartRateMonitorSensitivity = HeartRateMonitorSensitivity.HIGH
                }

                if (newHeartRateMonitorSensitivity.value != oldHeartRateMonitorSensitivity.value) {
                    heartRateMonitorSettings.sensitivityLevel = newHeartRateMonitorSensitivity
                    binding.btnSave.isEnabled = true
                } else {
                    binding.btnSave.isEnabled = false
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun showSuccessAnimation() {
        val ivSuccess = binding.ivSuccess
        ivSuccess.visibility = View.VISIBLE

        Glide.with(this)
            .load(R.drawable.gif_success)
            .into(ivSuccess)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).clear(ivSuccess)
                ivSuccess.visibility = View.GONE
            }
        }, 2200)
    }


    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}