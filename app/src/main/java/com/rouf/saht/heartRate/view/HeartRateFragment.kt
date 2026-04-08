package com.rouf.saht.heartRate.view

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.drawable.GradientDrawable
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.common.helper.HeartRateZoneUtils
import com.rouf.saht.R
import com.rouf.saht.common.helper.TimeUtil
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.databinding.BottomsheetSaveHeartRateBinding
import com.rouf.saht.databinding.FragmentHeartRateBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kibotu.heartrateometer.HeartRateOmeter

@AndroidEntryPoint
class HeartRateFragment : Fragment() {

    private val TAG = HeartRateFragment::class.java.simpleName
    private var _binding: FragmentHeartRateBinding? = null
    private val binding get() = _binding!!

    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private var subscription: CompositeDisposable? = null
    private lateinit var lineChart: LineChart
    private var bpmEntries = mutableListOf<Entry>()  // List to hold BPM entries for the graph
    private var heartRateMonitorData: HeartRateMonitorData = HeartRateMonitorData()

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var heartRateViewModel: HeartRateViewModel
    // Initialised with safe defaults so tapping Start before the async load completes never crashes
    private var heartRateMonitorSettings: HeartRateMonitorSettings = HeartRateMonitorSettings()

    private var heartRateTimer: CountDownTimer? = null
    private var isTimerStarted = false
    private var pulseAnimator: ObjectAnimator? = null

    private val noFingerHandler = Handler(Looper.getMainLooper())
    private val noFingerRunnable = Runnable {
        if (isTimerStarted) {
            Log.d(TAG, "No finger detected for 5 seconds — ending measurement")
            stopHeartRateMonitoringTimer()
            stopHeartRateMonitoring()
            initViewInActiveState()
            stopCircularProgress()
            setPartialHeartRateData()
            showSaveBPMRecordDialog(requireContext(), heartRateMonitorData)
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
        private const val NO_FINGER_TIMEOUT_MS = 5_000L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeartRateBinding.inflate(inflater, container, false)
        settingsViewModel = ViewModelProvider(this@HeartRateFragment)[SettingsViewModel::class.java]
        heartRateViewModel = ViewModelProvider(this@HeartRateFragment)[HeartRateViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            heartRateMonitorSettings = settingsViewModel.getHeartMonitorSettings()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customizeChartAppearance(binding.lineChart)
        initViewInActiveState()

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            onClick()
        }
    }

    private fun customizeChartAppearance(lineChart: LineChart) {
        val textColorBasedOnDarkMode = if (isDarkMode())
            Color.WHITE
        else
            Color.DKGRAY

        // Customize X axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(true)
        xAxis.textColor = textColorBasedOnDarkMode

        // Customize Y axis
        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(false)
        yAxis.setDrawLabels(true)
        yAxis.textColor = textColorBasedOnDarkMode

        // Disable right Y axis
        lineChart.axisRight.isEnabled = false

        lineChart.description.isEnabled = false
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissions(permissions, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onClick()
            } else {
                showPermissionsDeniedDialog()
            }
        }
    }

    private fun showPermissionsDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("Camera permissions are required to measure heart rate.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            .setNegativeButton("Cancel") { _, _ -> activity?.finish() }
            .show()
    }

    private fun onClick() {
        binding.btnMeasure.setOnClickListener {
            if (binding.btnMeasure.text == getString(R.string.start_monitoring)) {
                // Active state
                bpmEntries.clear()
                initViewActiveState()
                startHeartRateMonitoring()
            } else {
                // Inactive state
                initViewInActiveState()
                stopHeartRateMonitoring()
                stopHeartRateMonitoringTimer()
                stopCircularProgress()
            }
        }

        binding.ivHistory.setOnClickListener {
            val intent = Intent(activity, HeartRateHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun stopCircularProgress() {
        binding.progressBpm.progress = 0
        binding.progressBpm.isVisible = false
    }

    private fun initViewInActiveState() {
        binding.btnMeasure.text = getString(R.string.start_monitoring)
        binding.preview.isVisible = false
        binding.tvHeartRateZone.visibility = View.GONE
        binding.btnMeasure.backgroundTintList = ColorStateList.valueOf(BaseActivity.effectivePrimary(requireContext()))
    }

    private fun updateZoneBadge(bpm: Int) {
        val zone = HeartRateZoneUtils.getZone(requireContext(), bpm)
        if (zone == null) {
            binding.tvHeartRateZone.visibility = View.GONE
            return
        }
        val color = androidx.core.content.ContextCompat.getColor(requireContext(), zone.colorResId)
        binding.tvHeartRateZone.text = "${zone.name} · ${zone.description}"
        (binding.tvHeartRateZone.background as? GradientDrawable)?.setColor(color)
        if (binding.tvHeartRateZone.visibility != View.VISIBLE) {
            binding.tvHeartRateZone.alpha = 0f
            binding.tvHeartRateZone.visibility = View.VISIBLE
            binding.tvHeartRateZone.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun initViewActiveState() {
        binding.btnMeasure.text = getString(R.string.stop_monitoring)
        binding.preview.isVisible = true
        binding.btnMeasure.backgroundTintList = ColorStateList.valueOf(BaseActivity.effectiveSecondary(requireContext()))
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f, 1f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.tvHeartRate, scaleX, scaleY).apply {
            duration = 600
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        binding.tvHeartRate.scaleX = 1f
        binding.tvHeartRate.scaleY = 1f
    }

    private fun startHeartRateMonitoring() {
        lineChart = binding.lineChart
        subscription = CompositeDisposable()
        startPulseAnimation()

        Log.d(TAG, "startHeartRateMonitoring started with sensitivity: ${heartRateMonitorSettings.sensitivityLevel}")

        val bpmUpdates = HeartRateOmeter()
            /*
            * TODO: add setting to set average after seconds
            */
            .withAverageAfterSeconds(heartRateMonitorSettings.sensitivityLevel.value)
            .setFingerDetectionListener { detected -> onFingerChange(detected) }
            .bpmUpdates(binding.preview)
            .subscribe({ bpm ->
                if (bpm.value > 0) {
                    onBpm(bpm.value)

                    if (!isTimerStarted)
                        startHeartRateMonitoringTimer()
                }
            }, {
                Log.e(TAG, "Error receiving BPM updates", it)
            })

        subscription?.add(bpmUpdates)
        binding.btnMeasure.text = getString(R.string.stop_monitoring)
    }

    private fun stopHeartRateMonitoringTimer() {
        heartRateTimer?.cancel()
        heartRateTimer = null
        isTimerStarted = false
        noFingerHandler.removeCallbacks(noFingerRunnable)
        Log.d(TAG, "Timer stopped.")
    }

    private fun startHeartRateMonitoringTimer() {
        if (isTimerStarted) return

        isTimerStarted = true
        binding.progressBpm.max = heartRateMonitorSettings.duration - 1
        binding.progressBpm.progress = 0
        binding.progressBpm.isVisible = true   // show immediately, not after first tick
        val durationInMilliseconds = heartRateMonitorSettings.duration * 1000L
        var i = 0

        heartRateTimer = object : CountDownTimer(durationInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Time remaining: ${millisUntilFinished / 1000} seconds")
                if (i <= heartRateMonitorSettings.duration) {
                    binding.progressBpm.progress = i
                    i++
                }
            }

            override fun onFinish() {
                // Reset timer state FIRST so a second measurement can start cleanly
                stopHeartRateMonitoringTimer()
                stopHeartRateMonitoring()
                initViewInActiveState()
                stopCircularProgress()
                setPartialHeartRateData()
                showSaveBPMRecordDialog(requireContext(), heartRateMonitorData)
                Log.d(TAG, "onFinish: heartRateMonitorData: $heartRateMonitorData")
            }
        }.start()
    }

    private fun setPartialHeartRateData() {
        heartRateMonitorData.bpmGraphEntries = bpmEntries
        heartRateMonitorData.sensitivityLevel = heartRateMonitorSettings.sensitivityLevel
        heartRateMonitorData.duration = heartRateMonitorSettings.duration
        heartRateMonitorData.timeStamp = TimeUtil.getCurrentTimestamp()

        // Save zone data
        val zone = HeartRateZoneUtils.getZone(requireContext(), heartRateMonitorData.bpm)
        heartRateMonitorData.zone = zone?.name ?: ""
        heartRateMonitorData.zoneDistribution = HeartRateZoneUtils.calculateZoneDistribution(requireContext(), bpmEntries)
    }

    private fun stopHeartRateMonitoring() {
        subscription?.dispose()
        subscription = null
        stopPulseAnimation()
        binding.btnMeasure.text = getString(R.string.start_monitoring)
    }

    private fun onBpm(bpm: Int) {
        val bpmUnit = getString(R.string.bpm)
        binding.tvHeartRate.text = "$bpm ${bpmUnit}"

        updateZoneBadge(bpm)

        val entry = Entry(bpmEntries.size.toFloat(), bpm.toFloat())
        bpmEntries.add(entry)
        lifecycleScope.launch {
            heartRateMonitorData.bpm = bpm
        }

        updateGraph(binding.lineChart)
    }

    private fun updateGraph(lineChart: LineChart) {
        if (bpmEntries.size < 2) return

        val textColorBasedOnDarkMode = if (isDarkMode())
            Color.WHITE
        else
            Color.DKGRAY

        val dataSet = LineDataSet(bpmEntries, "Heart Rate (BPM)")

        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.color = Color.RED
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.RED
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawIcons(false)


        lineChart.clear()
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.legend.textColor = textColorBasedOnDarkMode
        lineChart.setTouchEnabled(true)
        lineChart.isDoubleTapToZoomEnabled = false

        lineChart.invalidate()
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }


    private fun onFingerChange(fingerDetected: Boolean) {
        if (fingerDetected) {
            noFingerHandler.removeCallbacks(noFingerRunnable)
        } else if (isTimerStarted) {
            // Schedule auto-end only if a measurement is in progress and a handler isn't already pending
            noFingerHandler.removeCallbacks(noFingerRunnable)
            noFingerHandler.postDelayed(noFingerRunnable, NO_FINGER_TIMEOUT_MS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (subscription == null && hasPermissions()) {
            onClick()
        }
    }

    override fun onPause() {
        stopHeartRateMonitoring()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showSaveBPMRecordDialog(context: Context, heartRateMonitorData: HeartRateMonitorData) {
        Log.d(TAG, "showSaveBPMRecordDialog: $heartRateMonitorData")
        val dialogBinding = BottomsheetSaveHeartRateBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, R.style.DialogThemeSize)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        val window = dialog.window ?: return
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val wlp = window.attributes
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT
        wlp.height = WindowManager.LayoutParams.WRAP_CONTENT
        wlp.gravity = Gravity.BOTTOM
        wlp.windowAnimations = R.style.bottomSheetAnimation
        window.attributes = wlp

        dialogBinding.tvHeartRate.text = "${heartRateMonitorData.bpm} BPM"
//        customizeChartAppearance(dialogBinding.lineChart)
        updateGraph(dialogBinding.lineChart)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.chipWalking.setOnClickListener {
            dialogBinding.etActivityPerformed.setText(
                dialogBinding.chipWalking.text
            )
        }

        dialogBinding.chipRunning.setOnClickListener {
            dialogBinding.etActivityPerformed.setText(
                dialogBinding.chipRunning.text
            )
        }

        dialogBinding.chipCycling.setOnClickListener {
            dialogBinding.etActivityPerformed.setText(
                dialogBinding.chipCycling.text
            )
        }

        dialogBinding.chipResting.setOnClickListener {
            dialogBinding.etActivityPerformed.setText(
                dialogBinding.chipResting.text
            )
        }

        dialogBinding.btnSave.setOnClickListener() {
            val activityPerformed: String = dialogBinding.etActivityPerformed.text.toString().trim()
            Log.d(TAG, "showSaveBPMRecordDialog: activityPerformed: $activityPerformed")

            if(activityPerformed.isNotEmpty() || activityPerformed.isNotBlank()) {
                // Call viewModel save method to list of heart rate data
                this.heartRateMonitorData.activityPerformed = dialogBinding.etActivityPerformed.text.toString()
                Log.d(TAG, "showSaveBPMRecordDialog: heart rate data -> ${this.heartRateMonitorData}")

                lifecycleScope.launch(Dispatchers.IO){
                    heartRateViewModel.saveHeartRateMonitorData(heartRateMonitorData)
                    Log.d(TAG, "showSaveBPMRecordDialog: dataList -> ${heartRateViewModel.getHeartRateMonitorData()}")
                }

                dialog.dismiss()
            } else {
                dialogBinding.etActivityPerformed.error = "Please provide valid activity"
            }
        }

        dialog.show()
    }
}
