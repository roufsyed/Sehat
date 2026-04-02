package com.rouf.saht.heartRate.view

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
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
        AlertDialog.Builder(requireContext())
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
        binding.tvFingerDetect.isVisible = false
        binding.btnMeasure.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_cornered_solid_red)
        binding.btnMeasure.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green_500)
    }

    private fun initViewActiveState() {
        binding.btnMeasure.text = getString(R.string.stop_monitoring)
        binding.preview.isVisible = true
        binding.tvFingerDetect.isVisible = true
        binding.btnMeasure.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_button_cornered_solid_red)
        binding.btnMeasure.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red_500)
    }

    private fun startHeartRateMonitoring() {
        lineChart = binding.lineChart
        subscription = CompositeDisposable()

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
        heartRateTimer?.cancel() // Cancel the timer if it is running
        heartRateTimer = null
        isTimerStarted = false
        Log.d(TAG, "Timer stopped.")
    }

    private fun startHeartRateMonitoringTimer() {
        if (isTimerStarted) {
            return
        }

        isTimerStarted = true
        binding.progressBpm.max = heartRateMonitorSettings.duration - 1
        val durationInMilliseconds = heartRateMonitorSettings.duration * 1000L
        var i = 0

        heartRateTimer = object : CountDownTimer(durationInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Time remaining: ${millisUntilFinished / 1000} seconds")
                binding.progressBpm.isVisible = isTimerStarted

                if (i <= heartRateMonitorSettings.duration) {
                    binding.progressBpm.progress = i
                    i++
                }

            }

            override fun onFinish() {
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
    }

    private fun stopHeartRateMonitoring() {
        subscription?.dispose()
        subscription = null
        binding.btnMeasure.text = getString(R.string.start_monitoring)
    }

    private fun onBpm(bpm: Int) {
        val bpmUnit = getString(R.string.bpm)
        binding.tvHeartRate.text = "$bpm ${bpmUnit}"

        val entry = Entry(bpmEntries.size.toFloat(), bpm.toFloat())
        bpmEntries.add(entry)
        lifecycleScope.launch {
            heartRateMonitorData.bpm = bpm
        }

        updateGraph(binding.lineChart)
    }

    private fun updateGraph(lineChart: LineChart) {
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
        // Only update the label — do NOT touch the timer here.
        // The library fires false "no finger" events intermittently even with a finger
        // correctly placed; stopping the timer on every such event causes it to keep
        // restarting from full duration whenever the next valid BPM arrives.
        binding.tvFingerDetect.text = if (fingerDetected) "Finger Detected" else "No Finger"
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
