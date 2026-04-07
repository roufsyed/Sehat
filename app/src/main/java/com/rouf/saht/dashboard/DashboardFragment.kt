package com.rouf.saht.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.google.android.material.color.MaterialColors
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.rouf.saht.common.helper.BMIUtils
import com.rouf.saht.common.helper.Util
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.FragmentDashboardBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var pedometerViewModel: PedometerViewModel
    private lateinit var heartRateViewModel: HeartRateViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        pedometerViewModel = ViewModelProvider(this)[PedometerViewModel::class.java]
        heartRateViewModel = ViewModelProvider(this)[HeartRateViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSteps()
        loadHeartRate()
        loadBmi()
        loadWeeklyChart()
    }

    private fun loadSteps() {
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = settingsViewModel.getPedometerSettings()
            binding.dashboardProgressRing.max = settings.stepGoal
            binding.tvDashboardGoal.text = "/ ${Util.formatWithCommas(settings.stepGoal)}"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pedometerViewModel.steps.collect { steps ->
                    binding.tvDashboardSteps.text = steps.toString()
                    binding.dashboardProgressRing.progress = steps.coerceAtMost(binding.dashboardProgressRing.max)
                }
            }
        }
    }

    private fun loadHeartRate() {
        heartRateViewModel.getHeartRateMonitorData()
        heartRateViewModel.heartRateMonitorData.observe(viewLifecycleOwner) { list ->
            val last = list?.lastOrNull()
            if (last != null) {
                binding.tvDashboardHeartRate.text = "${last.bpm} BPM"
                binding.tvDashboardHrActivity.text = last.activityPerformed
            }
        }
    }

    private fun loadBmi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val info = settingsViewModel.getPersonalInformation()
            val heightStr = info.height
            val weightStr = info.weight
            if (heightStr.isNotEmpty() && weightStr.isNotEmpty()) {
                val height = heightStr.toDoubleOrNull()?.let { BMIUtils.cmToMeter(it) }
                val weight = weightStr.toDoubleOrNull()
                if (height != null && weight != null && height > 0 && weight > 0) {
                    val bmi = BMIUtils.calculateBMI(weight, height)
                    binding.tvDashboardBmi.text = String.format("%.1f", bmi)
                    binding.tvDashboardBmiCategory.text = BMIUtils.getBMICategory(bmi)
                    binding.tvDashboardBmi.setTextColor(BMIUtils.getCategoryColor(requireContext(), bmi))
                }
            }
        }
    }

    private fun loadWeeklyChart() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                pedometerViewModel.getPedometerListFromDB()
            } ?: emptyList()

            setupWeeklyBarChart(list)
        }
    }

    private fun setupWeeklyBarChart(allData: List<PedometerData>) {
        val chart = binding.barChartWeekly
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Build a map of day-label -> total steps for the last 7 days
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            val dayLabel = dayFormat.format(cal.time)
            labels.add(dayLabel)

            val stepsForDay = allData
                .filter { it.date == dateStr }
                .sumOf { it.steps }

            entries.add(BarEntry((6 - i).toFloat(), stepsForDay.toFloat()))
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = BarDataSet(entries, "Steps").apply {
            color = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
            setDrawValues(false)
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                this.textColor = textColor
            }
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                this.textColor = textColor
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
