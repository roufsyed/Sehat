package com.rouf.saht.pedometer.repository

import android.util.Log
import com.rouf.saht.common.model.PedometerData
import io.paperdb.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedometerRepository @Inject constructor() {

    private val TAG: String? = PedometerRepository::class.java.simpleName

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    suspend fun updateSteps(currentSteps: Int) {
        withContext(Dispatchers.Default) {
            _steps.value = currentSteps
            Log.d("PedometerRepos", "updateSteps: ${steps.value}")
        }
    }

    private val _calories = MutableStateFlow(0.0)
    val calories: StateFlow<Double> = _calories.asStateFlow()

    suspend fun updateCalories(caloriesBurnt: Double) {
        withContext(Dispatchers.Default) {
            _calories.value = caloriesBurnt
            Log.d("PedometerRepos", "update Calories: ${calories.value}")
        }
    }

    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    suspend fun updateDistance(pdistance: Double) {
        withContext(Dispatchers.Default) {
            _distance.value = pdistance
            Log.d("PedometerRepos", "update distance: ${distance.value}")
        }
    }

    private val _totalExerciseDuration = MutableStateFlow(0.0)
    val totalExerciseDuration: StateFlow<Double> = _totalExerciseDuration.asStateFlow()

    suspend fun updateTotalExerciseDuration(ptotalExerciseDuration: Long) {
        withContext(Dispatchers.Default) {
            _totalExerciseDuration.value = ptotalExerciseDuration.toDouble()
            Log.d("PedometerRepos", "update totalExerciseDuration: ${totalExerciseDuration.value}")
        }
    }

    suspend fun updatePedometerDataInDB(pedometerData: PedometerData) {
        withContext(Dispatchers.Default) {
            Paper.book().write("pedometer_data", pedometerData)
            Log.d("PedometerData", "Pedometer Data: $pedometerData")
        }
    }

    suspend fun getPedometerDataFromDB(): PedometerData? = withContext(Dispatchers.IO) {
        Paper.book().read("pedometer_data")
    }

    suspend fun savePedometerDataToList(): Unit = withContext(Dispatchers.IO) {
        val pedometerDataList: MutableList<PedometerData> = Paper.book().read("pedometer_data_list") ?: mutableListOf()
        getPedometerDataFromDB()?.let { latestPedometerData ->
            if (latestPedometerData.steps > 0) {
                pedometerDataList.add(latestPedometerData)
            }
        }
        Paper.book().write("pedometer_data_list", pedometerDataList)
    }

    suspend fun getPedometerlist(): List<PedometerData>? = withContext(Dispatchers.IO) {
        return@withContext Paper.book().read("pedometer_data_list")
    }

    suspend fun filterPedometerListByDateRange(fromDate: Long, toDate: Long): List<PedometerData>? {
        val pedometerData: List<PedometerData>? = Paper.book().read<List<PedometerData>>("pedometer_data_list", emptyList())

        pedometerData.let { pedometerData ->
            return pedometerData?.filter { data ->
                data.timestamp in fromDate..toDate
            }
        }
    }

    suspend fun deletePedometerDataByPosition(position: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val PedometerDataList: MutableList<PedometerData>? = Paper.book().read("pedometer_data_list")
            Log.i(TAG, "deletePedometerDataDataByPosition: position -> $position")
            Log.i(TAG, "deletePedometerDataDataByPosition: PedometerDataList -> $PedometerDataList")

            if (position < 0 || position >= (PedometerDataList?.size
                    ?: emptyList<PedometerData>().size)
            ) {
                throw IndexOutOfBoundsException("Position out of bounds")
            }

            val updatedList = PedometerDataList?.filterIndexed { index, _ -> index != position }

            updatedList?.let { Paper.book().write("pedometer_data_list", it) }

            Log.d(TAG, "deletePedometerDataDataByPosition: Successfully deleted entry at position $position")
            true
        } catch (e: Exception) {
            Log.e(TAG, "deletePedometerDataDataByPosition: Error deleting heart rate monitor data", e)
            false
        }
    }


    private suspend fun resetPedometerDataInDB(): Unit = withContext(Dispatchers.IO) {
        Paper.book().delete("pedometer_data")
    }

    suspend fun resetData() {
        updateIsReset(true)
        resetPedometerDataInDB()
        _steps.value = 0
        _calories.value = 0.0
        Log.d(TAG, "resetData: \n steps: ${steps.value} \n calories: ${calories.value} ")
    }

    fun getSteps(): Float {
        return steps.value.toFloat()
    }

    val _isReset = MutableStateFlow(false)  // Use StateFlow instead of LiveData
    val isReset = _isReset.asStateFlow()

    fun updateIsReset(value: Boolean) {
        _isReset.value = value  // StateFlow is safe to update from any thread
    }


}