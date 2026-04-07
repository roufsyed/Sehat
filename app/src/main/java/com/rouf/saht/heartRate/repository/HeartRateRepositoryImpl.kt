package com.rouf.saht.heartRate.repository

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.rouf.saht.common.model.HeartRateMonitorData
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.heartRate.data.HeartRateData
import com.rouf.saht.heartRate.exception.CameraException
import com.rouf.saht.heartRate.sensor.CameraHeartRateAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.paperdb.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HeartRateRepositoryImpl @Inject constructor(
    private val cameraProvider: ProcessCameraProvider
) : HeartRateRepository {

    private val TAG: String = HeartRateRepositoryImpl::class.java.simpleName

    private val _heartRateFlow = MutableStateFlow<HeartRateData?>(null)
    private var camera: Camera? = null
    private val analyzer = CameraHeartRateAnalyzer()

    override suspend fun getHeartRateData(): Flow<HeartRateData> = _heartRateFlow
        .filterNotNull()
        .flowOn(Dispatchers.IO)

    @RequiresApi(35)
    override suspend fun startHeartRateMonitoring(lifecycleOwner: LifecycleOwner) {
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        Dispatchers.IO.asExecutor()
                    ) { image ->
                        analyzer.analyze(image) { _heartRateFlow.value = it }
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                camera?.cameraControl?.enableTorch(true)
            } catch (e: Exception) {
                throw CameraException("Failed to start camera: ${e.message}")
            }
        }
    }

    override suspend fun stopHeartRateMonitoring() {
        camera?.cameraControl?.enableTorch(false)
        cameraProvider.unbindAll()
        camera = null
    }

    override fun isMonitoring(): Boolean = camera != null

    override suspend fun saveHeartRateMonitorData(heartRateMonitorData: HeartRateMonitorData): Unit = withContext(Dispatchers.IO) {
        val currentData: MutableList<HeartRateMonitorData> = Paper.book().read("heart_rate_monitor_data") ?: mutableListOf()
        currentData.add(heartRateMonitorData)
        Paper.book().write("heart_rate_monitor_data", currentData)
    }

    override suspend fun getHeartRateMonitorData(): List<HeartRateMonitorData>? = withContext(Dispatchers.IO) {
        return@withContext Paper.book().read("heart_rate_monitor_data")
    }

    override suspend fun deleteHeartRateMonitorDataByTimestamp(timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val list: MutableList<HeartRateMonitorData>? = Paper.book().read("heart_rate_monitor_data")
            val updated = list?.filterNot { it.timeStamp == timestamp }
            updated?.let { Paper.book().write("heart_rate_monitor_data", it) }
            updated?.size != list?.size
        } catch (e: Exception) {
            Log.e(TAG, "deleteHeartRateMonitorDataByTimestamp: Error", e)
            false
        }
    }

    override suspend fun deleteHeartRateMonitorDataByPosition(position: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val heartRateDataList: MutableList<HeartRateMonitorData>? = Paper.book().read("heart_rate_monitor_data")
            Log.i(TAG, "deleteHeartRateMonitorDataByPosition: position -> $position")
            Log.i(TAG, "deleteHeartRateMonitorDataByPosition: heartRateDataList -> $heartRateDataList")

            if (position < 0 || position >= (heartRateDataList?.size
                    ?: emptyList<HeartRateMonitorData>().size)
            ) {
                throw IndexOutOfBoundsException("Position out of bounds")
            }

            val updatedList = heartRateDataList?.filterIndexed { index, _ -> index != position }

            updatedList?.let { Paper.book().write("heart_rate_monitor_data", it) }

            Log.d(TAG, "deleteHeartRateMonitorDataByPosition: Successfully deleted entry at position $position")
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteHeartRateMonitorDataByPosition: Error deleting heart rate monitor data", e)
            false
        }
    }
}


