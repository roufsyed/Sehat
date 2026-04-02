package com.rouf.saht.heartRate.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.rouf.saht.databinding.ActivityHeartRateHistoryBinding
import com.rouf.saht.heartRate.viewModel.HeartRateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HeartRateHistoryActivity : AppCompatActivity() {

    private val TAG: String = HeartRateHistoryActivity::class.java.simpleName
    private lateinit var binding: ActivityHeartRateHistoryBinding
    private lateinit var heartRateAdapter: HeartRateAdapter
    private lateinit var heartRateViewModel: HeartRateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHeartRateHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        heartRateViewModel = ViewModelProvider(this@HeartRateHistoryActivity)[HeartRateViewModel::class.java]


        setupRecyclerView()
        observer()
    }

    override fun onResume() {
        super.onResume()
        getHeartRateData()
    }

    private fun setupRecyclerView() {
        heartRateAdapter = HeartRateAdapter(this)
        binding.rvHeartRate.layoutManager = LinearLayoutManager(this)
        binding.rvHeartRate.adapter = heartRateAdapter
    }

    private fun getHeartRateData() {
        heartRateViewModel.getHeartRateMonitorData()
    }

    private fun observer() {
        heartRateViewModel.heartRateMonitorData.observe(this) { data ->
            val hearRateList = data ?: emptyList()
            binding.llEmptyView.isVisible = hearRateList.isEmpty()
            heartRateAdapter.submitList(hearRateList)
        }
    }
}