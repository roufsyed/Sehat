package com.rouf.saht.pedometer.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.pedometer.repository.PedometerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PedometerViewModel @Inject constructor(
    private val pedometerRepository: PedometerRepository
) : ViewModel() {

    private val TAG: String = PedometerViewModel::class.java.simpleName

    val steps = pedometerRepository.steps
    val calories = pedometerRepository.calories
    val distance = pedometerRepository.distance
    val totalExerciseDuration = pedometerRepository.totalExerciseDuration

    suspend fun resetData() {
        pedometerRepository.resetData()
    }

    private val _stateActive = MutableStateFlow(false)
    val stateActive: StateFlow<Boolean> = _stateActive.asStateFlow()

    suspend fun updateStateActive(currentState: Boolean) {
        withContext(Dispatchers.IO) {
            _stateActive.value = currentState
            Log.d("PedometerRepos", "updateStateActive: ${stateActive.value}")
        }
    }

    fun getActiveState(): Boolean {
        return stateActive.value
    }

    private val _pedometerGoal = MutableLiveData(10000)
    val pedometerGoal: MutableLiveData<Int> get() = _pedometerGoal

    suspend fun updatePedometerGoal(newGoal: Int) {
        _pedometerGoal.value = newGoal
    }

    suspend fun savePedometerDataToList() {
        pedometerRepository.savePedometerDataToList()
    }

    suspend fun getPedometerListFromDB(): List<PedometerData>? {
        return pedometerRepository.getPedometerlist()
    }

    suspend fun removePedometerDataFromList(position: Int): Boolean {
        return pedometerRepository.deletePedometerDataByPosition(position)
    }

    suspend fun deletePedometerDataByPosition(position: Int): Boolean {
        return pedometerRepository.deletePedometerDataByPosition(position)
    }

    suspend fun deletePedometerDataByTimestamp(timestamp: Long): Boolean {
        return pedometerRepository.deletePedometerDataByTimestamp(timestamp)
    }
}