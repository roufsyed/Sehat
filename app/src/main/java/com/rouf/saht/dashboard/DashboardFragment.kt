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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
        loadPedometerAnalytics()
        loadHeartRateAnalytics()
        // Combined analytics loaded after HR data is cached in loadHeartRateZones
    }

    private fun applyDashboardVisibility() {
        val b = _binding ?: return
        val showSteps = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_STEPS, true) != false
        val showHr = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_HEART_RATE, true) != false
        val showBmi = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_BMI, true) != false
        val showChart = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_WEEKLY_CHART, true) != false
        val showZones = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_HR_ZONES, true) != false

        b.cardSteps.visibility = if (showSteps) View.VISIBLE else View.GONE
        b.cardHeartRate.visibility = if (showHr) View.VISIBLE else View.GONE
        b.cardBmi.visibility = if (showBmi) View.VISIBLE else View.GONE
        b.cardWeeklyChart.visibility = if (showChart) View.VISIBLE else View.GONE

        // Hide entire row if both HR and BMI are hidden
        b.llHrBmiRow.visibility = if (!showHr && !showBmi) View.GONE else View.VISIBLE

        // Make cards full-width when the other is hidden
        val gap = (6 * resources.displayMetrics.density).toInt()
        val hrParams = b.cardHeartRate.layoutParams as android.widget.LinearLayout.LayoutParams
        val bmiParams = b.cardBmi.layoutParams as android.widget.LinearLayout.LayoutParams
        when {
            showHr && !showBmi -> { hrParams.setMargins(0, 0, 0, 0) }
            !showHr && showBmi -> { bmiParams.setMargins(0, 0, 0, 0) }
            else -> { hrParams.setMargins(0, 0, gap, 0); bmiParams.setMargins(gap, 0, 0, 0) }
        }
        b.cardHeartRate.layoutParams = hrParams
        b.cardBmi.layoutParams = bmiParams

        val showDistance = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_DISTANCE, true) != false
        val showCalories = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_CALORIES, true) != false
        val showDuration = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_ACTIVE_DURATION, true) != false
        b.cardDistance.visibility = if (showDistance) View.VISIBLE else View.GONE
        b.cardCalories.visibility = if (showCalories) View.VISIBLE else View.GONE
        b.cardActiveDuration.visibility = if (showDuration) View.VISIBLE else View.GONE

        val showBpmActivity = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_BPM_BY_ACTIVITY, true) != false
        val showPeakBpm = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_PEAK_BPM, true) != false
        val showRecovery = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_RECOVERY, true) != false
        b.cardBpmByActivity.visibility = if (showBpmActivity) View.VISIBLE else View.GONE
        b.cardPeakBpm.visibility = if (showPeakBpm) View.VISIBLE else View.GONE
        b.cardRecovery.visibility = if (showRecovery) View.VISIBLE else View.GONE

        val allHidden = !showSteps && !showHr && !showBmi && !showChart && !showZones
                && !showDistance && !showCalories && !showDuration
                && !showBpmActivity && !showPeakBpm && !showRecovery
                && Paper.book().read(DashboardSettingsActivity.PREF_SHOW_CORRELATION, true) == false
                && Paper.book().read(DashboardSettingsActivity.PREF_SHOW_WEEKLY_SUMMARY, true) == false
                && Paper.book().read(DashboardSettingsActivity.PREF_SHOW_RECORDS, true) == false
        b.llEmptyState.visibility = if (allHidden) View.VISIBLE else View.GONE
        b.llDashboardContent.visibility = if (allHidden) View.GONE else View.VISIBLE

        b.btnOpenDashboardSettings.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), DashboardSettingsActivity::class.java))
        }
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
        if (Paper.book().read(DashboardSettingsActivity.PREF_SHOW_HR_ZONES, true) == false) {
            // Still need to cache HR data for combined analytics
            heartRateViewModel.getHeartRateMonitorData()
            heartRateViewModel.heartRateMonitorData.observe(viewLifecycleOwner) { list ->
                if (!list.isNullOrEmpty()) {
                    cachedHrData = list
                    loadCombinedAnalytics()
                }
            }
            return
        }
        heartRateViewModel.getHeartRateMonitorData()
        heartRateViewModel.heartRateMonitorData.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) return@observe
            cachedHrData = list
            val b = _binding ?: return@observe
            b.cardHrZones.visibility = View.VISIBLE
            renderZoneChart(daysBack = 30) // default: 1 month
            setupZoneFilter()
            // Combined analytics depend on cached HR data
            loadCombinedAnalytics()
        }
    }

    private val zoneFilterOptions = listOf("Today", "1 Month", "3 Months", "6 Months", "1 Year")
    private val zoneFilterDays = listOf(0, 30, 90, 180, 365)

    private fun setupZoneFilter() {
        val b = _binding ?: return
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, zoneFilterOptions)
        b.actvZoneFilter.setAdapter(adapter)
        b.actvZoneFilter.setOnItemClickListener { _, _, position, _ ->
            renderZoneChart(daysBack = zoneFilterDays[position])
        }
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

    // ---- Pedometer Analytics ----

    private fun loadPedometerAnalytics() {
        val b = _binding ?: return
        val showDistance = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_DISTANCE, true) != false
        val showCalories = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_CALORIES, true) != false
        val showDuration = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_ACTIVE_DURATION, true) != false

        if (!showDistance && !showCalories && !showDuration) return

        viewLifecycleOwner.lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                pedometerViewModel.getPedometerListFromDB()
            } ?: emptyList()

            if (data.isEmpty() || _binding == null) return@launch

            if (showDistance) {
                setupDistanceChart(data, isMonthly = false)
                setupDistanceFilter(data)
            }
            if (showCalories) {
                setupCaloriesChart(data, isMonthly = false)
                setupCaloriesFilter(data)
            }
            if (showDuration) {
                setupActiveDuration(data)
            }
        }
    }

    private val periodOptions = listOf("Weekly", "Monthly")

    private fun setupDistanceFilter(data: List<PedometerData>) {
        val b = _binding ?: return
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periodOptions)
        b.actvDistanceFilter.setAdapter(adapter)
        b.actvDistanceFilter.setOnItemClickListener { _, _, pos, _ ->
            setupDistanceChart(data, isMonthly = pos == 1)
        }
    }

    private fun setupDistanceChart(allData: List<PedometerData>, isMonthly: Boolean) {
        val chart = _binding?.lineChartDistance ?: return
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val labels = mutableListOf<String>()
        val entries = mutableListOf<Entry>()

        val count = if (isMonthly) 30 else 7
        val labelFormat = if (isMonthly) java.text.SimpleDateFormat("d", java.util.Locale.getDefault())
            else java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

        for (i in (count - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            labels.add(labelFormat.format(cal.time))

            val km = allData.filter { it.date == dateStr }.sumOf { it.distanceMeters } / 1000.0
            entries.add(Entry(((count - 1) - i).toFloat(), km.toFloat()))
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = LineDataSet(entries, "Distance (km)").apply {
            color = BaseActivity.effectivePrimary(requireContext())
            setCircleColor(BaseActivity.effectivePrimary(requireContext()))
            circleRadius = 3f
            lineWidth = 2f
            setDrawValues(false)
            setDrawIcons(false)
            setDrawFilled(true)
            fillColor = BaseActivity.effectivePrimary(requireContext())
            fillAlpha = 40
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            clear()
            data = LineData(dataSet)
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
        }
    }

    private fun setupCaloriesFilter(data: List<PedometerData>) {
        val b = _binding ?: return
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periodOptions)
        b.actvCaloriesFilter.setAdapter(adapter)
        b.actvCaloriesFilter.setOnItemClickListener { _, _, pos, _ ->
            setupCaloriesChart(data, isMonthly = pos == 1)
        }
    }

    private fun setupCaloriesChart(allData: List<PedometerData>, isMonthly: Boolean) {
        val chart = _binding?.barChartCalories ?: return
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val labels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()

        val count = if (isMonthly) 30 else 7
        val labelFormat = if (isMonthly) java.text.SimpleDateFormat("d", java.util.Locale.getDefault())
            else java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

        for (i in (count - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            labels.add(labelFormat.format(cal.time))

            val cal_val = allData.filter { it.date == dateStr }.sumOf { it.caloriesBurned }
            entries.add(BarEntry(((count - 1) - i).toFloat(), cal_val.toFloat()))
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = BarDataSet(entries, "Calories").apply {
            color = ContextCompat.getColor(requireContext(), R.color.zone_vigorous)
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

    private fun setupActiveDuration(allData: List<PedometerData>) {
        val b = _binding ?: return
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recent = allData.filter { it.timestamp >= thirtyDaysAgo }

        if (recent.isEmpty()) {
            b.tvAvgDuration.text = "0"
            b.tvDurationSubtitle.text = "No data in the last 30 days"
            return
        }

        val totalMinutes = recent.sumOf { it.totalExerciseDuration } / 60000.0
        val avgMinPerDay = (totalMinutes / 30).toInt()
        b.tvAvgDuration.text = avgMinPerDay.toString()
        b.tvDurationSubtitle.text = "${recent.size} active days in the last 30 days"
    }

    // ---- Heart Rate Analytics ----

    private fun loadHeartRateAnalytics() {
        val b = _binding ?: return
        val showBpmAct = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_BPM_BY_ACTIVITY, true) != false
        val showPeak = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_PEAK_BPM, true) != false
        val showRecovery = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_RECOVERY, true) != false

        if (!showBpmAct && !showPeak && !showRecovery) return

        heartRateViewModel.getHeartRateMonitorData()
        heartRateViewModel.heartRateMonitorData.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty() || _binding == null) return@observe
            if (showBpmAct) setupBpmByActivity(list)
            if (showPeak) setupPeakBpmChart(list)
            if (showRecovery) setupRecoveryIndicator(list)
        }
    }

    private fun setupBpmByActivity(data: List<com.rouf.saht.common.model.HeartRateMonitorData>) {
        val chart = _binding?.hBarChartBpmActivity ?: return

        val grouped = data.filter { it.activityPerformed.isNotBlank() }
            .groupBy { it.activityPerformed }
            .mapValues { (_, items) -> items.map { it.bpm }.average().toFloat() }
            .toList()
            .sortedByDescending { it.second }

        if (grouped.isEmpty()) return
        _binding?.cardBpmByActivity?.visibility = View.VISIBLE

        val entries = grouped.mapIndexed { i, (_, avg) -> BarEntry(i.toFloat(), avg) }
        val labels = grouped.map { it.first }

        val activityColors = listOf(
            ContextCompat.getColor(requireContext(), R.color.zone_intense),
            ContextCompat.getColor(requireContext(), R.color.zone_vigorous),
            ContextCompat.getColor(requireContext(), R.color.zone_moderate),
            ContextCompat.getColor(requireContext(), R.color.zone_light),
            BaseActivity.effectivePrimary(requireContext()),
            BaseActivity.effectiveSecondary(requireContext())
        )

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = BarDataSet(entries, "").apply {
            colors = activityColors.take(grouped.size)
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = textColor
        }

        chart.apply {
            this.data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setFitBars(true)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelCount = labels.size
                setDrawGridLines(false)
                this.textColor = textColor
                textSize = 11f
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

    private fun setupPeakBpmChart(data: List<com.rouf.saht.common.model.HeartRateMonitorData>) {
        val chart = _binding?.lineChartPeakBpm ?: return

        val sorted = data.sortedBy { it.timeStamp }
        if (sorted.size < 2) return
        _binding?.cardPeakBpm?.visibility = View.VISIBLE

        val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        sorted.forEachIndexed { i, hr ->
            val peakBpm = hr.bpmGraphEntries.maxOfOrNull { it.y }?.toInt() ?: hr.bpm
            entries.add(Entry(i.toFloat(), peakBpm.toFloat()))
            labels.add(dateFormat.format(java.util.Date(hr.timeStamp)))
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = LineDataSet(entries, "Peak BPM").apply {
            color = ContextCompat.getColor(requireContext(), R.color.zone_intense)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.zone_intense))
            circleRadius = 3f
            lineWidth = 2f
            setDrawValues(false)
            setDrawIcons(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            clear()
            this.data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelCount = minOf(labels.size, 6)
                setDrawGridLines(false)
                this.textColor = textColor
                textSize = 10f
            }
            axisLeft.apply {
                setDrawGridLines(false)
                this.textColor = textColor
            }
            axisRight.isEnabled = false
            animateY(300)
        }
    }

    private fun setupRecoveryIndicator(data: List<com.rouf.saht.common.model.HeartRateMonitorData>) {
        val chart = _binding?.lineChartRecovery ?: return
        val b = _binding ?: return

        // Filter resting HR sessions only
        val restingSessions = data
            .filter { it.isResting || it.activityPerformed.equals("Resting", ignoreCase = true) }
            .sortedBy { it.timeStamp }

        if (restingSessions.size < 2) return
        b.cardRecovery.visibility = View.VISIBLE

        val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        restingSessions.forEachIndexed { i, hr ->
            entries.add(Entry(i.toFloat(), hr.bpm.toFloat()))
            labels.add(dateFormat.format(java.util.Date(hr.timeStamp)))
        }

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        val dataSet = LineDataSet(entries, "Resting BPM").apply {
            color = ContextCompat.getColor(requireContext(), R.color.zone_moderate)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.zone_moderate))
            circleRadius = 3f
            lineWidth = 2f
            setDrawValues(false)
            setDrawIcons(false)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.zone_moderate)
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            clear()
            this.data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelCount = minOf(labels.size, 6)
                setDrawGridLines(false)
                this.textColor = textColor
                textSize = 10f
            }
            axisLeft.apply {
                setDrawGridLines(false)
                this.textColor = textColor
            }
            axisRight.isEnabled = false
            animateY(300)
        }

        // Insight: compare first half avg to second half avg
        val half = restingSessions.size / 2
        val firstHalfAvg = restingSessions.take(half).map { it.bpm }.average()
        val secondHalfAvg = restingSessions.drop(half).map { it.bpm }.average()
        val diff = firstHalfAvg - secondHalfAvg

        val insight = when {
            diff > 3  -> "Your resting HR is dropping — great fitness progress!"
            diff < -3 -> "Your resting HR has increased — consider more rest"
            else      -> "Your resting HR is stable"
        }
        val insightColor = when {
            diff > 3  -> ContextCompat.getColor(requireContext(), R.color.zone_moderate)
            diff < -3 -> ContextCompat.getColor(requireContext(), R.color.zone_intense)
            else      -> textColor
        }
        b.tvRecoveryInsight.text = insight
        b.tvRecoveryInsight.setTextColor(insightColor)
    }

    // ---- Combined Analytics ----

    private fun loadCombinedAnalytics() {
        val b = _binding ?: return
        val showCorrelation = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_CORRELATION, true) != false
        val showSummary = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_WEEKLY_SUMMARY, true) != false
        val showRecords = Paper.book().read(DashboardSettingsActivity.PREF_SHOW_RECORDS, true) != false

        if (!showCorrelation && !showSummary && !showRecords) return

        viewLifecycleOwner.lifecycleScope.launch {
            val pedData = withContext(Dispatchers.IO) {
                pedometerViewModel.getPedometerListFromDB()
            } ?: emptyList()

            // Use HR data from LiveData observer (loaded by loadHeartRate/loadHeartRateZones)
            val hrData = cachedHrData

            if (_binding == null) return@launch

            if (showCorrelation) setupCorrelationChart(pedData, hrData)
            if (showSummary) setupWeeklySummary(pedData, hrData)
            if (showRecords) setupPersonalRecords(pedData, hrData)
        }
    }

    private fun setupCorrelationChart(
        pedData: List<PedometerData>,
        hrData: List<com.rouf.saht.common.model.HeartRateMonitorData>
    ) {
        val b = _binding ?: return
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        // Match same-day pedometer steps with HR BPM
        val stepsByDate = pedData.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.steps } }
        val hrByDate = hrData.groupBy { dateFormat.format(java.util.Date(it.timeStamp)) }
            .mapValues { (_, items) -> items.map { it.bpm }.average().toFloat() }

        val entries = mutableListOf<Entry>()
        for ((date, steps) in stepsByDate) {
            val avgBpm = hrByDate[date] ?: continue
            entries.add(Entry(steps.toFloat(), avgBpm))
        }

        if (entries.size < 3) return
        b.cardCorrelation.visibility = View.VISIBLE

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY

        // Use LineChart with circles only (no lines) as scatter plot
        val dataSet = LineDataSet(entries.sortedBy { it.x }, "").apply {
            color = Color.TRANSPARENT
            setDrawCircles(true)
            setCircleColor(BaseActivity.effectivePrimary(requireContext()))
            circleRadius = 5f
            setDrawCircleHole(false)
            lineWidth = 0f
            setDrawValues(false)
            setDrawIcons(false)
        }

        b.chartCorrelation.apply {
            clear()
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                this.textColor = textColor
                axisMinimum = 0f
            }
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                this.textColor = textColor
            }
            axisRight.isEnabled = false
            animateY(300)
        }

        // Simple correlation insight
        val avgSteps = entries.map { it.x }.average()
        val highStepEntries = entries.filter { it.x > avgSteps }
        val lowStepEntries = entries.filter { it.x <= avgSteps }
        val highBpm = if (highStepEntries.isNotEmpty()) highStepEntries.map { it.y }.average() else 0.0
        val lowBpm = if (lowStepEntries.isNotEmpty()) lowStepEntries.map { it.y }.average() else 0.0
        val diff = highBpm - lowBpm

        b.tvCorrelationInsight.text = when {
            diff > 5 -> "High-step days show ~${diff.toInt()} BPM higher heart rate"
            diff < -5 -> "More steps correlate with lower resting HR"
            else -> "No strong correlation between steps and heart rate"
        }
    }

    private fun setupWeeklySummary(
        pedData: List<PedometerData>,
        hrData: List<com.rouf.saht.common.model.HeartRateMonitorData>
    ) {
        val b = _binding ?: return
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        val weekPed = pedData.filter { it.timestamp >= weekAgo }
        val weekHr = hrData.filter { it.timeStamp >= weekAgo }

        b.cardWeeklySummary.visibility = View.VISIBLE

        val totalSteps = weekPed.sumOf { it.steps }
        val totalDistance = weekPed.sumOf { it.distanceMeters } / 1000.0
        val totalCalories = weekPed.sumOf { it.caloriesBurned }

        b.tvSummarySteps.text = Util.formatWithCommas(totalSteps)
        b.tvSummaryDistance.text = String.format("%.1f", totalDistance)
        b.tvSummaryCalories.text = String.format("%.0f", totalCalories)
        b.tvSummaryHrSessions.text = weekHr.size.toString()

        if (weekHr.isNotEmpty()) {
            b.tvSummaryAvgBpm.text = (weekHr.map { it.bpm }.average().toInt()).toString()
            val zoneCounts = mutableMapOf<String, Int>()
            for (hr in weekHr) {
                val zone = hr.zone.ifBlank {
                    HeartRateZoneUtils.getZone(requireContext(), hr.bpm)?.name ?: ""
                }
                if (zone.isNotBlank()) zoneCounts[zone] = (zoneCounts[zone] ?: 0) + 1
            }
            b.tvSummaryTopZone.text = zoneCounts.maxByOrNull { it.value }?.key ?: "--"
        } else {
            b.tvSummaryAvgBpm.text = "--"
            b.tvSummaryTopZone.text = "--"
        }
    }

    private fun setupPersonalRecords(
        pedData: List<PedometerData>,
        hrData: List<com.rouf.saht.common.model.HeartRateMonitorData>
    ) {
        val b = _binding ?: return
        if (pedData.isEmpty() && hrData.isEmpty()) return

        b.cardRecords.visibility = View.VISIBLE

        // Max steps in a single day
        val stepsByDate = pedData.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.steps } }
        val maxSteps = stepsByDate.values.maxOrNull() ?: 0
        b.tvRecordMaxSteps.text = Util.formatWithCommas(maxSteps)

        // Peak BPM ever
        val peakBpm = hrData.maxOfOrNull { hr ->
            hr.bpmGraphEntries.maxOfOrNull { it.y.toInt() } ?: hr.bpm
        } ?: 0
        b.tvRecordMaxBpm.text = peakBpm.toString()

        // Longest consecutive active day streak
        val activeDates = pedData.map { it.date }.distinct().sorted()
        var maxStreak = 0
        var currentStreak = if (activeDates.isNotEmpty()) 1 else 0
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        for (i in 1 until activeDates.size) {
            val prev = dateFormat.parse(activeDates[i - 1])
            val curr = dateFormat.parse(activeDates[i])
            if (prev != null && curr != null) {
                val diffDays = (curr.time - prev.time) / (24 * 60 * 60 * 1000)
                if (diffDays == 1L) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
        }
        maxStreak = maxOf(maxStreak, currentStreak)
        b.tvRecordStreak.text = maxStreak.toString()

        // Max distance in a day
        val distByDate = pedData.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.distanceMeters } / 1000.0 }
        val maxDist = distByDate.values.maxOrNull() ?: 0.0
        b.tvRecordMaxDistance.text = String.format("%.1f", maxDist)

        // Max calories in a day
        val calByDate = pedData.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.caloriesBurned } }
        val maxCal = calByDate.values.maxOrNull() ?: 0.0
        b.tvRecordMaxCalories.text = String.format("%.0f", maxCal)

        // Days goal was hit
        val goalDays = stepsByDate.count { (_, steps) -> steps >= 10000 }
        b.tvRecordGoalDays.text = goalDays.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
