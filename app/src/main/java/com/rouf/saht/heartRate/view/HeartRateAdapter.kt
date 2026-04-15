package com.rouf.saht.heartRate.view

import android.content.Context
import com.rouf.saht.common.helper.HeartRateZoneUtils
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.rouf.saht.common.helper.TimeUtil
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.databinding.ItemHeartRateBinding

class HeartRateAdapter(private val context: Context) : RecyclerView.Adapter<HeartRateAdapter.HeartRateViewHolder>() {

    private var heartRateList = listOf<HeartRateMonitorData>()

    fun submitList(data: List<HeartRateMonitorData>) {
        heartRateList = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeartRateViewHolder {
        val binding = ItemHeartRateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeartRateViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: HeartRateViewHolder, position: Int) {
        holder.bind(heartRateList[position])
    }

    override fun getItemCount(): Int = heartRateList.size

    inner class HeartRateViewHolder(private val binding: ItemHeartRateBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(heartRateData: HeartRateMonitorData) {
            binding.tvHeartRate.text = "${heartRateData.bpm}\nBPM"
            binding.tvTime.text = TimeUtil.timestampToDateTime(heartRateData.timeStamp)
            binding.tvActivityPerformed.text = heartRateData.activityPerformed
            binding.lineChart.clear()
            customizeChartAppearance(binding.lineChart)
            updateGraph(binding.lineChart, heartRateData)

            val zone = HeartRateZoneUtils.getZone(itemView.context, heartRateData.bpm)
            if (zone != null) {
                binding.tvZone.text = "${zone.name} · ${zone.description}"
                binding.tvZone.setTextColor(
                    androidx.core.content.ContextCompat.getColor(itemView.context, zone.colorResId)
                )
                binding.tvZone.visibility = android.view.View.VISIBLE
            } else {
                binding.tvZone.visibility = android.view.View.GONE
            }
        }

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_ID) onItemClick(pos)
            }
        }

        private fun onItemClick(position: Int) {
            val context = itemView.context
            val intent = Intent(context, HeartRateDetailActivity::class.java)
            intent.putExtra("heartRateData", heartRateList[position])
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