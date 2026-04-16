package com.rouf.saht.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rouf.saht.common.model.Gender
import com.rouf.saht.common.model.HeartRateMonitorSensitivity
import com.rouf.saht.common.model.HeartRateMonitorSettings
import com.rouf.saht.common.model.PedometerSensitivity
import com.rouf.saht.common.model.PedometerSettings
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.common.model.WeightEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingRepository: SettingRepository
): ViewModel() {

    private val _personalInformation = MutableLiveData<PersonalInformation>()
    val personalInformation: LiveData<PersonalInformation> = _personalInformation

    suspend fun savePersonalInformation(personalInformation: PersonalInformation) {
        settingRepository.savePersonalInformation(personalInformation)
    }

    suspend fun getPersonalInformation(): PersonalInformation {
        val data = settingRepository.getPersonalInformation() ?: getEmptyPersonalInformation()
        _personalInformation.postValue(data)
        return data
    }

    fun getEmptyPersonalInformation(): PersonalInformation {
        return PersonalInformation(
            name = "",
            gender = Gender.MALE,
            height = "",
            heightUnit = "cm",
            weight = "",
            weightUnit = "kg",
            selectedYear = 0,
            selectedMonth = 0,
            selectedDay = 0,
            formatedDate = "",
            age = ""
        )
    }

    /*
    * Pedometer Settings
    * */
    private val _pedometerSettings = MutableLiveData<PedometerSettings>()
    val pedometerSettings: LiveData<PedometerSettings> = _pedometerSettings

    suspend fun savePedometerSettings(pedometerSettings: PedometerSettings) {
        settingRepository.savePedometerSettings(pedometerSettings)
    }

    suspend fun getPedometerSettings(): PedometerSettings {
        val data = settingRepository.getPedometerSettings() ?: getDefaultPedometerSettings()
        _pedometerSettings.postValue(data)
        return data
    }

    private fun getDefaultPedometerSettings(): PedometerSettings {
        return PedometerSettings(
            stepGoal = 10000,
            unit = "steps",
            sensitivityLevel = PedometerSensitivity.LOW
        )
    }

    /*
    * Heart rate monitor Settings
    * */
    private val _heartRateMonitorSettings = MutableLiveData<HeartRateMonitorSettings>()
    val heartRateMonitorSettings: LiveData<HeartRateMonitorSettings> = _heartRateMonitorSettings

    suspend fun saveHeartMonitorSettings(heartRateMonitorSettings: HeartRateMonitorSettings) {
        settingRepository.saveHeartMonitorSettings(heartRateMonitorSettings)
    }

    suspend fun getHeartMonitorSettings(): HeartRateMonitorSettings {
        val data = settingRepository.getHeartRateMonitorSettings() ?: getDefaultHeartMonitorSettings()
        _heartRateMonitorSettings.postValue(data)
        return data
    }

    private fun getDefaultHeartMonitorSettings(): HeartRateMonitorSettings {
        return HeartRateMonitorSettings(
            duration = 30,
            unit = "seconds",
            sensitivityLevel = HeartRateMonitorSensitivity.MEDIUM
        )
    }

    /*
    * Weight history
    * */
    suspend fun appendWeightEntry(entry: WeightEntry) {
        settingRepository.appendWeightEntry(entry)
    }

    suspend fun getWeightHistory(): List<WeightEntry> {
        return settingRepository.getWeightHistory()
    }

}