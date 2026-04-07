package com.rouf.saht.heartRate.view

import android.os.Bundle
import com.rouf.saht.common.activity.BaseActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.rouf.saht.R
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.databinding.ActivityHeartRateHistoryBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class HeartRateHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHeartRateHistoryBinding
    private lateinit var heartRateAdapter: HeartRateAdapter
    private lateinit var heartRateViewModel: HeartRateViewModel

    private var fullList: List<HeartRateMonitorData> = emptyList()
    private var customFromDate: Long = 0L
    private var customToDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        heartRateViewModel = ViewModelProvider(this)[HeartRateViewModel::class.java]

        setupRecyclerView()
        setupFilterChips()
        observer()
    }

    override fun onResume() {
        super.onResume()
        heartRateViewModel.getHeartRateMonitorData()
    }

    private fun setupRecyclerView() {
        heartRateAdapter = HeartRateAdapter(this)
        binding.rvHeartRate.layoutManager = LinearLayoutManager(this)
        binding.rvHeartRate.adapter = heartRateAdapter
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
                showData(fullList.filter { it.timeStamp in weekStart..now })
            }
            R.id.chip_month -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                showData(fullList.filter { it.timeStamp in monthStart..now })
            }
            R.id.chip_custom -> showDateRangePicker()
        }
    }

    private fun observer() {
        heartRateViewModel.heartRateMonitorData.observe(this) { data ->
            fullList = data ?: emptyList()
            val checkedId = binding.chipGroupFilter.checkedChipId
            if (checkedId == R.id.chip_custom && customFromDate > 0) {
                showData(fullList.filter { it.timeStamp in customFromDate..customToDate })
            } else {
                applyFilter(checkedId)
            }
        }
    }

    private fun showData(data: List<HeartRateMonitorData>) {
        val sorted = data.sortedByDescending { it.timeStamp }
        binding.llEmptyView.isVisible = sorted.isEmpty()
        heartRateAdapter.submitList(sorted)
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            customFromDate = selection.first ?: return@addOnPositiveButtonClickListener
            customToDate = (selection.second ?: return@addOnPositiveButtonClickListener) + 86400000L - 1
            showData(fullList.filter { it.timeStamp in customFromDate..customToDate })
        }
        picker.addOnNegativeButtonClickListener {
            binding.chipAll.isChecked = true
        }
        picker.show(supportFragmentManager, "HR_DATE_RANGE")
    }
}
