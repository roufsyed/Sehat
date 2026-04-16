package com.rouf.saht.pedometer.view

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.rouf.saht.R
import com.rouf.saht.common.helper.TimeUtil
import com.rouf.saht.common.helper.Util
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.ItemHeartRateBinding
import com.rouf.saht.databinding.ItemPedometerBinding
import com.rouf.saht.heartRate.view.HeartRateDetailActivity

class PedometerHistoryAdapter(private val context: Context) : RecyclerView.Adapter<PedometerHistoryAdapter.PedometerViewHolder>() {

    private var pedometerList = listOf<PedometerData>()

    fun submitList(data: List<PedometerData>) {
        pedometerList = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedometerViewHolder {
        val binding = ItemPedometerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedometerViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: PedometerViewHolder, position: Int) {
        holder.bind(pedometerList[position])
    }

    override fun getItemCount(): Int = pedometerList.size

    inner class PedometerViewHolder(private val binding: ItemPedometerBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(pedometerData: PedometerData) {
            val stepsText = "${pedometerData.steps}\nsteps"
            binding.tvSteps.text = Util.boldSubstring(stepsText, pedometerData.steps.toString())

            val dateText = "Date: ${pedometerData.date}"
            binding.tvDate.text = Util.boldSubstring(
                dateText,
                pedometerData.date
            )

            val durationText = "Duration ⏳: ${Util.formatDuration(pedometerData.totalExerciseDuration.toDouble())}"
            binding.tvActivityDuration.text = Util.boldSubstring(
                durationText,
                Util.formatDuration(pedometerData.totalExerciseDuration.toDouble())
            )

            val caloriesText = "Calories 🔥: ${pedometerData.caloriesBurned} kcal"
            binding.tvCalories.text = Util.boldSubstring(caloriesText, "${pedometerData.caloriesBurned} kcal")

            val distanceText = "Distance 🏃‍♂️: ${Util.formatDistance(pedometerData.distanceMeters)}"
            binding.tvDistance.text = Util.boldSubstring(distanceText, Util.formatDistance(pedometerData.distanceMeters))

            val goal = pedometerData.goal
            if (goal > 0) {
                val goalHit = pedometerData.steps >= goal
                val progressText = "${Util.formatWithCommas(pedometerData.steps)} / ${Util.formatWithCommas(goal)} steps"
                binding.tvGoal.text = if (goalHit) "Goal: $progressText ✓" else "Goal: $progressText"
                binding.tvGoal.setTextColor(
                    if (goalHit) ContextCompat.getColor(context, R.color.zone_moderate)
                    else ContextCompat.getColor(context, R.color.dark_grey)
                )
                binding.tvGoal.visibility = View.VISIBLE
            } else {
                binding.tvGoal.visibility = View.GONE
            }
        }

        init {
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }

        private fun onItemClick(position: Int) {
            val context = itemView.context
            val intent = Intent(context, PedometerDetailActivity::class.java)
            intent.putExtra("pedometerData", pedometerList[position])
            intent.putExtra("position", position.toString())
            context.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun customizeChartAppearance(lineChart: LineChart) {
        val textColorBasedOnDarkMode = if (isDarkModeEnabled())
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

        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.isFocusable = false
        lineChart.isClickable = false
        lineChart.isDragEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.isScaleXEnabled = false
        lineChart.isScaleYEnabled = false
        lineChart.isDoubleTapToZoomEnabled = false
        lineChart.setPinchZoom(false)
    }

    private fun updateGraph(lineChart: LineChart, heartRateData: HeartRateMonitorData) {
        if (heartRateData.bpmGraphEntries.size < 2) return

        val textColorBasedOnDarkMode = if (isDarkModeEnabled())
            Color.WHITE
        else
            Color.DKGRAY

        val dataSet = LineDataSet(heartRateData.bpmGraphEntries, "Heart Rate (BPM)")

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

        lineChart.notifyDataSetChanged()
        lineChart.invalidate()
    }

    private fun isDarkModeEnabled(): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
}