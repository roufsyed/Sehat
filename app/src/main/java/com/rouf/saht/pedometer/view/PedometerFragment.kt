package com.rouf.saht.pedometer.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.R
import com.rouf.saht.common.helper.BMIUtils
import com.rouf.saht.common.helper.Util
import com.rouf.saht.common.model.PedometerSensitivity
import com.rouf.saht.common.model.PedometerSettings
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.databinding.FragmentPedometerBinding
import com.rouf.saht.pedometer.service.PedometerForegroundService
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import com.rouf.saht.setting.SettingsViewModel
import com.rouf.saht.setting.view.PersonalInformationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class PedometerFragment : Fragment() {

    private val TAG = PedometerFragment::class.java.simpleName
    private var _binding: FragmentPedometerBinding? = null
    private val binding get() = _binding!!

    private val ACTIVITY_RECOGNITION_PERMISSION_CODE = 123
    private val REQUEST_NOTIFICATION_PERMISSION: Int = 1

    private lateinit var pedometerViewModel: PedometerViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var pedometerSettings: PedometerSettings
    private lateinit var pedometerSensitivity: PedometerSensitivity
    private var activeState: Boolean = false

    private var BMI_FLAG = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPedometerBinding.inflate(inflater, container, false)

        pedometerViewModel = ViewModelProvider(this@PedometerFragment)[PedometerViewModel::class.java]
        settingsViewModel = ViewModelProvider(this@PedometerFragment)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            pedometerSettings = settingsViewModel.getPedometerSettings()
            pedometerSensitivity = pedometerSettings.sensitivityLevel
            Log.d(TAG, "onCreate in lifecycleScope: pedometerSettings -> $pedometerSettings")
        }

        activeState = pedometerViewModel.getActiveState()

        initViews()
        onClick()
        observers()

        lifecycleScope.launch {
            Log.d(TAG, "onDestroy: getPedometerListFromFB -> ${pedometerViewModel.getPedometerListFromDB()}")
        }

        return binding.root

    }

    private fun initViews() {
        val stateActive = pedometerViewModel.getActiveState()

        if (stateActive) {
            initViewActiveState()
        } else {
            initViewInActiveState()
        }

    }

    private fun setBMI(personalInformation: PersonalInformation) {
        val heightStr = personalInformation.height
        val weightStr = personalInformation.weight

        // Validate height and weight
        if (heightStr.isEmpty() || weightStr.isEmpty()) {
            binding.tvBmi.text = "BMI: " + getString(R.string.invalid_data)
            binding.tvBmi.setTextColor(BaseActivity.effectiveSecondary(requireContext()))

            BMI_FLAG = false

            return

        } else {
            // Parse height and weight
            val height = heightStr.toDoubleOrNull()?.let { BMIUtils.cmToMeter(it) }
            val weight = weightStr.toDoubleOrNull()

            if (height == null || weight == null || height <= 0 || weight <= 0) {
                throw IllegalArgumentException("Invalid height or weight values")
            }

            // Calculate BMI
            val bmi = BMIUtils.calculateBMI(weight, height)
            val formattedBmi = String.format("%.1f", bmi)
            val bmiCategory = BMIUtils.getBMICategory(bmi)
            val categoryColor = BMIUtils.getCategoryColor(requireContext(), bmi)

            // Update UI
            binding.tvBmi.text = "Your BMI: $formattedBmi | Diagnosis: $bmiCategory"
            binding.tvBmi.setTextColor(categoryColor)

            BMI_FLAG = true
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
            settingsViewModel.getPedometerSettings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onClick() {
        binding.btnStartStop.setOnClickListener {
            if (isActivityRecognitionPermissionGranted()) {
                if (isNotificationPermissionGranted()) {
                    val btnState: String = binding.btnStartStop.text.toString()
                    if (btnState == "Start") {
                        togglePedometer()
                    } else {
                        Toast.makeText(activity, "Long press to stop", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    requestActivityNotificationPermission()
                }
            } else {
                requestActivityRecognitionPermission()
            }
        }

        binding.btnReset.setOnClickListener {
            Toast.makeText(activity, "Long press to reset steps", Toast.LENGTH_SHORT).show()
        }

        binding.btnStartStop.setOnLongClickListener {
            if (isActivityRecognitionPermissionGranted()) {
                if (isNotificationPermissionGranted()) {
                    togglePedometer()
                } else {
                    requestActivityNotificationPermission()
                }
            } else {
                requestActivityRecognitionPermission()
            }
            return@setOnLongClickListener true
        }

        binding.btnReset.setOnLongClickListener {
            resetStepsState()
            stopForegroundService()
            initViewInActiveState()
            return@setOnLongClickListener true
        }

        binding.ivHistory.setOnClickListener() {
            val intent = Intent(activity, PedometerHistoryActivity::class.java)
            startActivity(intent)
        }

        binding.tvBmi.setOnClickListener {
            if (BMI_FLAG) {
                // TODO: create some flow later
            } else {
                val intent = Intent(activity, PersonalInformationActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun togglePedometer() {
        val toggleText = binding.btnStartStop.text.toString()

        if (toggleText == "Start") {
            initViewActiveState()
            initiateForegroundService()
            lifecycleScope.launch {
                pedometerViewModel.updateStateActive(true)
            }
        } else {
            initViewInActiveState()
            stopForegroundService()
            lifecycleScope.launch {
                pedometerViewModel.updateStateActive(false)
            }
        }
    }



    private fun initViewInActiveState() {
        val btnStartStop = binding.btnStartStop
        btnStartStop.text = getString(R.string.start)
        btnStartStop.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_cornered_solid_red)
        btnStartStop.backgroundTintList = ColorStateList.valueOf(BaseActivity.effectivePrimary(requireContext()))

        // setting bg_circular color
        val shapeDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circular) as GradientDrawable
        val strokeColor = ContextCompat.getColor(requireContext(), R.color.dark_grey)
        shapeDrawable.setStroke(8, strokeColor)
        binding.containerPedometer.background = shapeDrawable
    }

    private fun initViewActiveState() {
        val btnStartStop = binding.btnStartStop
        val secondaryColor = BaseActivity.effectiveSecondary(requireContext())
        val primaryColor = BaseActivity.effectivePrimary(requireContext())

        btnStartStop.text = getString(R.string.stop)
        btnStartStop.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_cornered_solid_red)
        btnStartStop.backgroundTintList = ColorStateList.valueOf(secondaryColor)

        // setting bg_circular color
        val shapeDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circular) as GradientDrawable
        shapeDrawable.setStroke(8, primaryColor)
        binding.containerPedometer.background = shapeDrawable
    }


    private fun observers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedometerViewModel.steps.collect { steps ->
                    updateStepCount(steps)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedometerViewModel.calories.collect { caloriesBurnt->
                    updateCalorieCount(caloriesBurnt)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedometerViewModel.distance.collect { distanceCount->
                    updateDistanceCount(distanceCount)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedometerViewModel.totalExerciseDuration.collect { durationCount->
                    updateDurationCount(durationCount)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.personalInformation.observe(viewLifecycleOwner) { personalInformation ->
                    setBMI(personalInformation)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.pedometerSettings.observe(viewLifecycleOwner) { settings ->
                    binding.tvGoalSteps.text = "/ ${Util.formatWithCommas(settings.stepGoal)} steps"
                }
            }
        }
    }

    private fun updateStepCount(stepCount: Int) {
        binding.tvStepsTaken.text = stepCount.toString()
    }

    private fun updateCalorieCount(caloriesBurnt: Double) {
        val fullText = "Calories 🔥: \n${Util.roundToTwoDecimalPlaces(caloriesBurnt.toFloat())} kcal"
        val dataToBold = "${Util.roundToTwoDecimalPlaces(caloriesBurnt.toFloat())} kcal"
        binding.tvCalories.text = Util.boldSubstring(fullText, dataToBold)
    }

    private fun updateDistanceCount(distance: Double) {
        val fullText = "Distance \uD83C\uDFC3\u200D\u2642\uFE0F: \n${Util.formatDistance(distance)}"
        val dataToBold = Util.formatDistance(distance)
        binding.tvDistance.text = Util.boldSubstring(fullText, dataToBold)
    }

    private fun updateDurationCount(durationInMilliSeconds: Double) {
        val fullText = "Time ⏳: \n${Util.formatDuration(durationInMilliSeconds)}"
        val dataToBold = Util.formatDuration(durationInMilliSeconds)
        binding.tvDuration.text = Util.boldSubstring(fullText, dataToBold)
    }

    private fun initiateForegroundService() {
        val startIntent = Intent(requireContext(), PedometerForegroundService::class.java)
        startIntent.putExtra("sensitivity_level", pedometerSensitivity)
        ContextCompat.startForegroundService(requireContext(), startIntent)
    }

    private fun stopForegroundService() {
        savePedometerDataToList()
        val stopIntent = Intent(requireContext(), PedometerForegroundService::class.java)
        requireContext().stopService(stopIntent)
    }

    private fun savePedometerDataToList() {
        lifecycleScope.launch {
            pedometerViewModel.savePedometerDataToList()
        }
    }

    private fun isActivityRecognitionPermissionGranted(): Boolean {
            return ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVITY_RECOGNITION_PERMISSION_CODE
            )
        }
    }

    private fun requestActivityNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    private fun resetStepsState() {
        lifecycleScope.launch(Dispatchers.IO) {
            pedometerViewModel.resetData()
        }
    }

    private fun saveData() {
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
//        editor.putFloat("key1", pedometerViewModel.stepCount.value?.toFloat() ?: 0f)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("key1", 0f)
    }
}
