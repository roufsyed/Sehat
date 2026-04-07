package com.rouf.saht.pedometer.view

import android.os.Bundle
import com.rouf.saht.common.activity.BaseActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.rouf.saht.R
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.ActivityPedometerHisotryBinding
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

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
            val checkedId = binding.chipGroupFilter.checkedChipId
            if (checkedId == R.id.chip_custom && customFromDate > 0) {
                showData(fullList.filter { it.timestamp in customFromDate..customToDate })
            } else {
                applyFilter(checkedId)
            }
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
