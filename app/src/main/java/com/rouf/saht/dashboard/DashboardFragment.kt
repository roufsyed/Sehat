package com.rouf.saht.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.rouf.saht.R
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.setting.view.DashboardSettingsActivity
import com.rouf.saht.common.helper.BMIUtils
import com.rouf.saht.common.helper.HeartRateZoneUtils
import com.rouf.saht.common.helper.Util
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.FragmentDashboardBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.paperdb.Paper
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

        applyDashboardVisibility()
        loadSteps()
        loadHeartRate()
        loadBmi()
        loadWeeklyChart()
        loadHeartRateZones()
    }

    private fun applyDashboardVisibility() {
        val b = _binding ?: return
        b.cardSteps.visibility = if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_STEPS, true) != false) View.VISIBLE else View.GONE
        b.cardHeartRate.visibility = if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_HEART_RATE, true) != false) View.VISIBLE else View.GONE
        b.cardBmi.visibility = if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_BMI, true) != false) View.VISIBLE else View.GONE
        b.cardWeeklyChart.visibility = if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_WEEKLY_CHART, true) != false) View.VISIBLE else View.GONE
        // HR Zones card visibility is handled in loadHeartRateZones() since it also depends on data availability
    }

    private fun loadSteps() {
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = settingsViewModel.getPedometerSettings()
            _binding?.dashboardProgressRing?.max = settings.stepGoal
            _binding?.tvDashboardGoal?.text = "/ ${Util.formatWithCommas(settings.stepGoal)}"
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

                val zone = HeartRateZoneUtils.getZone(requireContext(), last.bpm)
                if (zone != null) {
                    binding.tvDashboardHrZone.text = "${zone.name} · ${zone.description}"
                    binding.tvDashboardHrZone.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), zone.colorResId)
                    )
                    binding.tvDashboardHrZone.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    private fun loadBmi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val info = settingsViewModel.getPersonalInformation()
            val b = _binding ?: return@launch
            val heightStr = info.height
            val weightStr = info.weight
            if (heightStr.isNotEmpty() && weightStr.isNotEmpty()) {
                val height = heightStr.toDoubleOrNull()?.let { BMIUtils.cmToMeter(it) }
                val weight = weightStr.toDoubleOrNull()
                if (height != null && weight != null && height > 0 && weight > 0) {
                    val bmi = BMIUtils.calculateBMI(weight, height)
                    b.tvDashboardBmi.text = String.format("%.1f", bmi)
                    b.tvDashboardBmiCategory.text = BMIUtils.getBMICategory(bmi)
                    b.tvDashboardBmi.setTextColor(BMIUtils.getCategoryColor(requireContext(), bmi))
                }
            }
        }
    }

    private var cachedPedometerData: List<PedometerData> = emptyList()

    private fun loadWeeklyChart() {
        viewLifecycleOwner.lifecycleScope.launch {
            cachedPedometerData = withContext(Dispatchers.IO) {
                pedometerViewModel.getPedometerListFromDB()
            } ?: emptyList()

            if (_binding != null) {
                setupStepsChart(isMonthly = false)
                setupChartFilter()
            }
        }
    }

    private fun setupChartFilter() {
        val b = _binding ?: return
        val options = listOf("Weekly", "Monthly")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        b.actvStepsFilter.setAdapter(adapter)
        b.actvStepsFilter.setOnItemClickListener { _, _, position, _ ->
            setupStepsChart(isMonthly = position == 1)
        }
    }

    private fun setupStepsChart(isMonthly: Boolean) {
        val chart = _binding?.barChartWeekly ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val labels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()

        if (isMonthly) {
            val dayFormat = SimpleDateFormat("d", Locale.getDefault())
            val count = 30
            for (i in (count - 1) downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = dateFormat.format(cal.time)
                val label = dayFormat.format(cal.time)
                labels.add(label)

                val steps = cachedPedometerData
                    .filter { it.date == dateStr }
                    .sumOf { it.steps }
                entries.add(BarEntry(((count - 1) - i).toFloat(), steps.toFloat()))
            }
        } else {
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = dateFormat.format(cal.time)
                val label = dayFormat.format(cal.time)
                labels.add(label)

                val steps = cachedPedometerData
                    .filter { it.date == dateStr }
                    .sumOf { it.steps }
                entries.add(BarEntry((6 - i).toFloat(), steps.toFloat()))
            }
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = BarDataSet(entries, "Steps").apply {
            color = BaseActivity.effectivePrimary(requireContext())
            setDrawValues(false)
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = if (isMonthly) 0.8f else 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelCount = if (isMonthly) 10 else 7
                setDrawGridLines(false)
                this.textColor = textColor
            }
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                this.textColor = textColor
            }
            axisRight.isEnabled = false
            animateY(300)
            invalidate()
        }
    }

    private var cachedHrData: List<com.rouf.saht.common.model.HeartRateMonitorData> = emptyList()

    private fun loadHeartRateZones() {
        if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_HR_ZONES, true) == false) return
        heartRateViewModel.getHeartRateMonitorData()
        heartRateViewModel.heartRateMonitorData.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) return@observe
            cachedHrData = list
            val b = _binding ?: return@observe
            b.cardHrZones.visibility = View.VISIBLE
            renderZoneChart(daysBack = 30) // default: 1 month
            setupZoneFilter()
        }
    }

    private fun setupZoneFilter() {
        val b = _binding ?: return
        b.chipZoneToday.setOnCheckedChangeListener { _, checked -> if (checked) renderZoneChart(daysBack = 0) }
        b.chipZoneMonth.setOnCheckedChangeListener { _, checked -> if (checked) renderZoneChart(daysBack = 30) }
        b.chipZone3M.setOnCheckedChangeListener { _, checked -> if (checked) renderZoneChart(daysBack = 90) }
        b.chipZone6M.setOnCheckedChangeListener { _, checked -> if (checked) renderZoneChart(daysBack = 180) }
        b.chipZone1Y.setOnCheckedChangeListener { _, checked -> if (checked) renderZoneChart(daysBack = 365) }
    }

    private fun renderZoneChart(daysBack: Int) {
        val b = _binding ?: return

        val cutoff = if (daysBack == 0) {
            // Today: start of current day
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            System.currentTimeMillis() - daysBack.toLong() * 24 * 60 * 60 * 1000
        }

        val filtered = cachedHrData.filter { it.timeStamp >= cutoff }
        if (filtered.isEmpty()) {
            b.pieChartZones.clear()
            b.llZoneLegend.removeAllViews()
            b.tvAvgBpm.text = "--"
            b.tvSessionCount.text = "0"
            b.tvTopZone.text = "--"
            return
        }

        val zoneCounts = mutableMapOf<String, Float>()
        var totalBpm = 0
        var sessionsWithZones = 0

        for (data in filtered) {
            totalBpm += data.bpm
            if (data.zone.isNotBlank()) sessionsWithZones++
            for ((zone, pct) in data.zoneDistribution) {
                zoneCounts[zone] = (zoneCounts[zone] ?: 0f) + pct
            }
        }

        if (sessionsWithZones == 0) {
            for (data in filtered) {
                val zone = HeartRateZoneUtils.getZone(requireContext(), data.bpm) ?: continue
                zoneCounts[zone.name] = (zoneCounts[zone.name] ?: 0f) + 1f
            }
        }

        if (zoneCounts.isEmpty()) {
            b.pieChartZones.clear()
            b.llZoneLegend.removeAllViews()
            b.tvAvgBpm.text = (totalBpm / filtered.size).toString()
            b.tvSessionCount.text = filtered.size.toString()
            b.tvTopZone.text = "--"
            return
        }

        b.tvAvgBpm.text = (totalBpm / filtered.size).toString()
        b.tvSessionCount.text = filtered.size.toString()
        b.tvTopZone.text = zoneCounts.maxByOrNull { it.value }?.key ?: "--"

        val zoneColorMap = mapOf(
            "Resting" to R.color.zone_light,
            "Moderate" to R.color.zone_moderate,
            "Vigorous" to R.color.zone_vigorous,
            "Intense" to R.color.zone_intense
        )

        val entries = zoneCounts.map { (name, value) -> PieEntry(value, name) }
        val colors = zoneCounts.keys.map { name ->
            ContextCompat.getColor(requireContext(), zoneColorMap[name] ?: R.color.dark_grey)
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
            sliceSpace = 2f
        }

        b.pieChartZones.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)
            setCenterText("Zones")
            setCenterTextSize(14f)
            setCenterTextColor(textColor)
            setTouchEnabled(false)
            animateY(300)
            invalidate()
        }

        b.llZoneLegend.removeAllViews()
        val total = zoneCounts.values.sum()
        for (zoneName in listOf("Resting", "Moderate", "Vigorous", "Intense")) {
            val value = zoneCounts[zoneName] ?: continue
            val pct = if (total > 0) (value / total * 100).toInt() else 0
            val colorRes = zoneColorMap[zoneName] ?: R.color.dark_grey
            val color = ContextCompat.getColor(requireContext(), colorRes)

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (10 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt()
                ).also { it.marginEnd = (8 * resources.displayMetrics.density).toInt() }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(color)
                }
            }
            val label = TextView(requireContext()).apply {
                text = "$zoneName  $pct%"
                textSize = 13f
                setTextColor(textColor)
            }
            row.addView(dot)
            row.addView(label)
            b.llZoneLegend.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
