package com.rouf.saht.pedometer.view

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.rouf.saht.common.activity.BaseActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.rouf.saht.R
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.ActivityPedometerHisotryBinding
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class PedometerHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityPedometerHisotryBinding
    private lateinit var pedometerHistoryAdapter: PedometerHistoryAdapter
    private lateinit var pedometerViewModel: PedometerViewModel

    private var fullList: List<PedometerData> = emptyList()
    private var customFromDate: Long = 0L
    private var customToDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedometerHisotryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedometerViewModel = ViewModelProvider(this)[PedometerViewModel::class.java]

        setupRecyclerView()
        setupFilterChips()
    }

    private fun setupRecyclerView() {
        pedometerHistoryAdapter = PedometerHistoryAdapter(this)
        binding.rvPedometer.layoutManager = LinearLayoutManager(this)
        binding.rvPedometer.adapter = pedometerHistoryAdapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            applyFilter(checkedIds.firstOrNull())
        }
    }

    private fun applyFilter(checkedId: Int?) {
        val now = System.currentTimeMillis()
        when (checkedId) {
            R.id.chip_all -> showData(fullList)
            R.id.chip_week -> {
                val weekStart = now - 7L * 24 * 60 * 60 * 1000
                showData(fullList.filter { it.timestamp in weekStart..now })
            }
            R.id.chip_month -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                showData(fullList.filter { it.timestamp in monthStart..now })
            }
            R.id.chip_custom -> showDateRangePicker()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            fullList = pedometerViewModel.getPedometerListFromDB() ?: emptyList()
            renderTrendChart(fullList)
            val checkedId = binding.chipGroupFilter.checkedChipId
            if (checkedId == R.id.chip_custom && customFromDate > 0) {
                showData(fullList.filter { it.timestamp in customFromDate..customToDate })
            } else {
                applyFilter(checkedId)
            }
        }
    }

    /**
     * Renders a 30-day step trend line using the full unfiltered dataset.
     * Hidden when there are no steps recorded in the last 30 days.
     * Always reflects the last 30 days regardless of which filter chip is active.
     */
    private fun renderTrendChart(allData: List<PedometerData>) {
        if (allData.isEmpty()) {
            binding.cardTrendChart.visibility = View.GONE
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("d", Locale.getDefault())
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val count = 30
        var hasData = false

        for (i in (count - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(cal.time)
            labels.add(labelFormat.format(cal.time))

            val steps = allData.filter { it.date == dateStr }.sumOf { it.steps }
            if (steps > 0) hasData = true
            entries.add(Entry(((count - 1) - i).toFloat(), steps.toFloat()))
        }

        if (!hasData) {
            binding.cardTrendChart.visibility = View.GONE
            return
        }

        binding.cardTrendChart.visibility = View.VISIBLE

        val isDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.DKGRAY
        val primaryColor = BaseActivity.effectivePrimary(this)

        val dataSet = LineDataSet(entries, "Steps").apply {
            color = primaryColor
            setCircleColor(primaryColor)
            circleRadius = 2.5f
            lineWidth = 2f
            setDrawValues(false)
            setDrawIcons(false)
            setDrawFilled(true)
            fillColor = primaryColor
            fillAlpha = 40
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChartStepTrend.apply {
            clear()
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelCount = 10
                setDrawGridLines(false)
                this.textColor = textColor
                textSize = 10f
            }
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                this.textColor = textColor
                textSize = 10f
            }
            axisRight.isEnabled = false
            animateY(300)
        }
    }

    private fun showData(data: List<PedometerData>) {
        val sorted = data.sortedByDescending { it.timestamp }
        binding.llEmptyView.isVisible = sorted.isEmpty()
        pedometerHistoryAdapter.submitList(sorted)
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            customFromDate = selection.first ?: return@addOnPositiveButtonClickListener
            customToDate = (selection.second ?: return@addOnPositiveButtonClickListener) + 86400000L - 1
            showData(fullList.filter { it.timestamp in customFromDate..customToDate })
        }
        picker.addOnNegativeButtonClickListener {
            binding.chipAll.isChecked = true
        }
        picker.show(supportFragmentManager, "PEDOMETER_DATE_RANGE")
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
