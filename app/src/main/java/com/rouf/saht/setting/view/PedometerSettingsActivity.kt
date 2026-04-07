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
import com.rouf.saht.common.activity.BaseActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rouf.saht.R
import com.rouf.saht.common.PedometerStepsConstants
import com.rouf.saht.common.model.PedometerSensitivity
import com.rouf.saht.common.model.PedometerSettings
import com.rouf.saht.databinding.ActivityPedometerSettingsBinding
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class PedometerSettingsActivity : BaseActivity() {
    private val TAG: String = PedometerSettingsActivity::class.java.simpleName
    private lateinit var binding: ActivityPedometerSettingsBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var pedometerSettings: PedometerSettings

    private var oldStepGoal by Delegates.notNull<Int>()
    private lateinit var oldPedometerSensitivity : PedometerSensitivity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPedometerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsViewModel = ViewModelProvider(this@PedometerSettingsActivity)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            pedometerSettings = settingsViewModel.getPedometerSettings()

            oldStepGoal = pedometerSettings.stepGoal
            oldPedometerSensitivity = pedometerSettings.sensitivityLevel

            initViews()
            onClick()
        }

    }

    private fun initViews() {
        val stepGoal: Int = pedometerSettings.stepGoal
        val pedometerSensitivity: PedometerSensitivity = pedometerSettings.sensitivityLevel

        binding.etStepGoal.setText(String.format(stepGoal.toString()))
        Log.d(TAG, "initViews: pedometerSensitivity -> $pedometerSensitivity")
        when(pedometerSensitivity) {
            PedometerSensitivity.LOW -> binding.seekBar.progress = 0
            PedometerSensitivity.MEDIUM -> binding.seekBar.progress = 1
            PedometerSensitivity.HIGH -> binding.seekBar.progress = 2
        }
    }

    private fun onClick() {

        binding.chip4000.setOnClickListener {
            binding.etStepGoal.setText(String.format(PedometerStepsConstants.STEPS_4000.toString()))
        }

        binding.chip10000.setOnClickListener {
            binding.etStepGoal.setText(String.format(PedometerStepsConstants.STEPS_10000.toString()))
        }

        binding.chip12000.setOnClickListener {
            binding.etStepGoal.setText(String.format(PedometerStepsConstants.STEPS_12000.toString()))
        }

        binding.chip15000.setOnClickListener {
            binding.etStepGoal.setText(String.format(PedometerStepsConstants.STEPS_15000.toString()))
        }

        binding.etStepGoal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val stepText = charSequence.toString().trim()

                if (stepText.isNotEmpty()) {
                    val steps = stepText.toIntOrNull()

                    if (steps != null && steps > 0) {
                        if (steps != oldStepGoal) {
                            binding.btnSave.isEnabled = true
                        } else {
                            binding.btnSave.isEnabled = false
                        }
                    } else {
                        binding.etStepGoal.error = "Please enter a valid daily goal"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    binding.etStepGoal.error = "Daily goal cannot be empty"
                    binding.btnSave.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        binding.btnSave.setOnClickListener {
            val stepGoal = binding.etStepGoal.text.toString().trim()
            val pedometerSensitivitySeekbarProgress = binding.seekBar.progress
            var pedometerSensitivity: PedometerSensitivity = oldPedometerSensitivity

            when(pedometerSensitivitySeekbarProgress) {
                PedometerSensitivity.LOW.value -> pedometerSensitivity = PedometerSensitivity.LOW
                PedometerSensitivity.MEDIUM.value -> pedometerSensitivity = PedometerSensitivity.MEDIUM
                PedometerSensitivity.HIGH.value -> pedometerSensitivity = PedometerSensitivity.HIGH
            }

            Log.d(TAG, "duration: $stepGoal, pedometerSensitivitySeekbarProgress: $pedometerSensitivitySeekbarProgress, pedometerSensitivity: ${pedometerSensitivity.name}")

            pedometerSettings.stepGoal = stepGoal.toInt()
            pedometerSettings.sensitivityLevel = pedometerSensitivity

            lifecycleScope.launch {
                settingsViewModel.savePedometerSettings(pedometerSettings)
            }
            binding.btnSave.isEnabled = false

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.Main) {
                showSuccessAnimation()
            }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                var newPedometerSensitivity: PedometerSensitivity = oldPedometerSensitivity

                when(progress) {
                    PedometerSensitivity.LOW.value -> newPedometerSensitivity = PedometerSensitivity.LOW
                    PedometerSensitivity.MEDIUM.value -> newPedometerSensitivity = PedometerSensitivity.MEDIUM
                    PedometerSensitivity.HIGH.value -> newPedometerSensitivity = PedometerSensitivity.HIGH
                }

                if (newPedometerSensitivity.value != oldPedometerSensitivity.value) {
                    pedometerSettings.sensitivityLevel = newPedometerSensitivity
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
            .load(R.drawable.ic_success)
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