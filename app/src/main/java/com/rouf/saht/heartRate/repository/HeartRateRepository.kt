package com.rouf.saht.heartRate.repository

import androidx.lifecycle.LifecycleOwner
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.heartRate.data.HeartRateData
import kotlinx.coroutines.flow.Flow

interface HeartRateRepository {
    suspend fun getHeartRateData(): Flow<HeartRateData>
    suspend fun startHeartRateMonitoring(lifecycleOwner: LifecycleOwner)
    suspend fun stopHeartRateMonitoring()
    fun isMonitoring(): Boolean

    suspend fun saveHeartRateMonitorData(heartRateMonitorData: HeartRateMonitorData)
    suspend fun getHeartRateMonitorData(): List<HeartRateMonitorData>?
    suspend fun deleteHeartRateMonitorDataByPosition(position: Int): Boolean
    suspend fun deleteHeartRateMonitorDataByTimestamp(timestamp: Long): Boolean

    companion object {
        const val MIN_CONFIDENCE_THRESHOLD = 0.7f
        const val SAMPLING_RATE_MS = 50L // 20fps
        const val MEASUREMENT_WINDOW_MS = 15000L // 15 seconds window
    }
}

