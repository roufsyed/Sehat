package com.rouf.saht.heartRate.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import java.util.Locale
import androidx.annotation.RequiresApi
import com.rouf.saht.common.activity.BaseActivity
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
import com.rouf.saht.databinding.ActivityHeartRateDetailBinding
import com.rouf.saht.databinding.DialogConfirmationBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HeartRateDetailActivity : BaseActivity() {
    private val TAG: String = HeartRateDetailActivity::class.java.simpleName
    private lateinit var binding: ActivityHeartRateDetailBinding
    private lateinit var heartRateViewModel: HeartRateViewModel
    private var heartRateData: HeartRateMonitorData? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeartRateDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        heartRateViewModel = ViewModelProvider(this@HeartRateDetailActivity)[HeartRateViewModel::class.java]

        heartRateData = intent.getParcelableExtra<HeartRateMonitorData>("heartRateData")
        heartRateData?.let { data ->
            initView(data)
            lifecycleScope.launch { loadComparison(data) }
        }

        onClick()
    }

    private fun onClick() {
        binding.btnDelete.setOnClickListener {
            showConfirmationDialog(this@HeartRateDetailActivity)
        }
    }

    private fun showConfirmationDialog(context: Context) {
        val dialogBinding = DialogConfirmationBinding.inflate(LayoutInflater.from(context))
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

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            Log.d(TAG, "showConfirmationDialog: Confirmed")
            lifecycleScope.launch {
                heartRateData?.let { data ->
                    val isDeleted: Boolean = heartRateViewModel.deleteByTimestamp(data.timeStamp)
                    if (isDeleted) finish()
                    else
                        Log.e(TAG, "showConfirmationDialog: Failed to delete")
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initView(heartRateData: HeartRateMonitorData) {
        customizeChartAppearance(binding.lineChart)
        updateGraph(binding.lineChart, heartRateData.bpmGraphEntries)

        binding.dateValue.text = TimeUtil.timestampToDateTime(heartRateData.timeStamp)
        binding.durationValue.text = heartRateData.duration.toString() + "sec"
        binding.activityValue.text = heartRateData.activityPerformed
        binding.bpmValue.text = heartRateData.bpm.toString()
        binding.sensitivityValue.text = heartRateData.sensitivityLevel.name
    }

    private fun customizeChartAppearance(lineChart: LineChart) {
        val textColorBasedOnDarkMode = if (isDarkMode())
            Color.WHITE
        else
            Color.DKGRAY

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(true)
        xAxis.textColor = textColorBasedOnDarkMode

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(false)
        yAxis.setDrawLabels(true)
        yAxis.textColor = textColorBasedOnDarkMode

        lineChart.axisRight.isEnabled = false

        lineChart.description.isEnabled = false
    }

    private fun updateGraph(lineChart: LineChart, bpmGraphEntries: MutableList<Entry>) {
        if (bpmGraphEntries.size < 2) return

        val textColorBasedOnDarkMode = if (isDarkMode())
            Color.WHITE
        else
            Color.DKGRAY

        val dataSet = LineDataSet(bpmGraphEntries, "Heart Rate (BPM)")

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

    /**
     * Loads all saved sessions, finds others with the same activity, and shows
     * how this session's BPM compares to the per-activity average.
     * Hidden when the activity label is blank or when this is the only session of its type.
     */
    private suspend fun loadComparison(current: HeartRateMonitorData) {
        val activity = current.activityPerformed.trim()
        if (activity.isBlank()) {
            binding.cardComparison.visibility = View.GONE
            return
        }

        val allSessions = heartRateViewModel.getAllHeartRateData()
        val sameSessions = allSessions.filter {
            it.activityPerformed.equals(activity, ignoreCase = true) &&
            it.timeStamp != current.timeStamp
        }

        if (sameSessions.isEmpty()) {
            binding.cardComparison.visibility = View.GONE
            return
        }

        val activityAvg = Math.round(sameSessions.map { it.bpm }.average()).toInt()
        val delta = current.bpm - activityAvg

        val activityLabel = activity.lowercase(Locale.getDefault())
        binding.tvThisSession.text = "This session: ${current.bpm} BPM"
        binding.tvActivityAverage.text = "Your $activityLabel average: $activityAvg BPM"
        binding.tvComparisonDelta.text = when {
            delta > 0 -> "$delta BPM above your average"
            delta < 0 -> "${-delta} BPM below your average"
            else      -> "Exactly at your average"
        }
        binding.cardComparison.visibility = View.VISIBLE
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}