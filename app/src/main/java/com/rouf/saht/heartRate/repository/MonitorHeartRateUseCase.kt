package com.rouf.saht.heartRate.repository

import androidx.lifecycle.LifecycleOwner
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.heartRate.data.HeartRateData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MonitorHeartRateUseCase @Inject constructor(
    private val repository: HeartRateRepository
) {
    suspend operator fun invoke(lifecycleOwner: LifecycleOwner): Flow<HeartRateData> {
        repository.startHeartRateMonitoring(lifecycleOwner)
        return repository.getHeartRateData()
    }

    suspend fun saveHeartRateMonitorData(heartRateMonitorData: HeartRateMonitorData) {
        repository.saveHeartRateMonitorData(heartRateMonitorData)
    }

    suspend fun getHeartRateMonitorData(): List<HeartRateMonitorData>? {
        return repository.getHeartRateMonitorData()
    }

    suspend fun deleteHeartRateMonitorDataByPosition(position: Int): Boolean {
        return repository.deleteHeartRateMonitorDataByPosition(position)
    }

    suspend fun deleteByTimestamp(timestamp: Long): Boolean {
        return repository.deleteHeartRateMonitorDataByTimestamp(timestamp)
    }
}