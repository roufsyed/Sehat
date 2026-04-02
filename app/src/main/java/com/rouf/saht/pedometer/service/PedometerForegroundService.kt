package com.rouf.saht.pedometer.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.rouf.saht.common.helper.CalorieCalculator
import com.rouf.saht.common.helper.NotificationHelper
import com.rouf.saht.common.helper.TimeUtil
import com.rouf.saht.common.helper.Util
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.common.model.PedometerSensitivity
import com.rouf.saht.pedometer.repository.PedometerRepository
import com.rouf.saht.setting.SettingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt


@AndroidEntryPoint
class PedometerForegroundService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "PedometerForegroundService"
        private const val NOTIFICATION_ID = 1

        const val VERY_HIGH_SENSITIVITY = SensorManager.SENSOR_DELAY_FASTEST  // 0 microseconds
        const val HIGH_SENSITIVITY = SensorManager.SENSOR_DELAY_GAME          // 20,000 microseconds
        const val MEDIUM_SENSITIVITY = SensorManager.SENSOR_DELAY_UI          // 60,000 microseconds
        const val LOW_SENSITIVITY = SensorManager.SENSOR_DELAY_NORMAL         // 200,000 microseconds
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var sensitivity = LOW_SENSITIVITY
    private var listener: SensorEventListener? = null

    private val calorieCalculator = CalorieCalculator(weightKg = 70f, isRunning = false)

    private val pedometerData: PedometerData by lazy {
        PedometerData(
            steps = 0,
            goal = 0,
            distanceMeters = 0.0,
            caloriesBurned = 0.0,
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
        )
    }

    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var stepOffset = 0 // Tracks the steps at reset
    private var isReset = false
    private val defaultStepLength = 0.762

    @Inject lateinit var pedometerRepository: PedometerRepository
    @Inject lateinit var settingRepository: SettingRepository
    private lateinit var pedometerSensitivity: PedometerSensitivity


    init {
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        Log.d(TAG, "onCreate: \n totalSteps: $totalSteps \n prevTotalSteps: $previousTotalSteps ")

        serviceScope.launch {
            val pedometerSettings = settingRepository.getPedometerSettings()
            pedometerSensitivity = pedometerSettings?.sensitivityLevel ?: PedometerSensitivity.MEDIUM

            sensitivity = when (pedometerSensitivity) {
                PedometerSensitivity.LOW -> {
                    LOW_SENSITIVITY
                }

                PedometerSensitivity.MEDIUM -> {
                    MEDIUM_SENSITIVITY
                }

                PedometerSensitivity.HIGH -> {
                    HIGH_SENSITIVITY
                }
            }
            Log.d(TAG, "onCreate: sensitivity: $sensitivity")
        }

        // Start foreground service immediately
        val notificationHelper = NotificationHelper(this)
        val initialNotification = notificationHelper.getServiceNotification("Steps: 0" ,"Calories Burnt: 0.0 kcal")
        startForeground(NOTIFICATION_ID, initialNotification)

//        loadData()
        initializeStepSensor()
        setSensitivity(sensitivity)
    }

    private fun setSensitivity(newSensitivity: Int) {
        sensitivity = newSensitivity
        sensorManager?.unregisterListener(this)
        val registered = sensorManager?.registerListener(this, stepSensor, sensitivity)
        Log.i(TAG, "Changed sensitivity to $sensitivity, registration: $registered")
    }


    private fun initializeStepSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.e(TAG, "No step counter sensor available on this device")
            stopSelf()
        } else {
            Log.d(TAG, "initializeStepSensor: sensitivity: $sensitivity")
            sensorManager?.registerListener(this, stepSensor, sensitivity)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    @SuppressLint("ServiceCast")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            totalSteps = it.values[0]
            Log.d(TAG, "onSensorChanged: it.values[0] -> ${it.values}")

            val isSameDay = TimeUtil.isSameDay(
                lastKnownTimeStamp = pedometerData.endTime,
                presentTimeStamp = TimeUtil.getCurrentTimestamp()
            )

            if (!isReset) {
                // First run or no reset
                stepOffset = totalSteps.roundToInt() // Set initial offset
                isReset = true // Mark as initialized
            }

            val currentSteps: Int = (totalSteps - stepOffset).roundToInt()

            val caloriesBurned = Util.roundToTwoDecimalPlaces(
                    calorieCalculator.calculateCalories(currentSteps)
            )

            Log.d(TAG, "Steps taken: $currentSteps")

            // Update the notification with current step count
            val notificationHelper = NotificationHelper(this)
            val notification = notificationHelper.getServiceNotification("Steps: $currentSteps", "Calories: $caloriesBurned kcal")

            // Update existing notification instead of calling startForeground again
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

//            saveData() // Save the steps persistently
            CoroutineScope(Dispatchers.IO).launch {
                updatePedometerDataInDB(currentSteps, caloriesBurned)
            }

            serviceScope.launch {
                pedometerRepository.updateSteps(currentSteps)
                pedometerRepository.updateCalories(caloriesBurned)
                pedometerRepository.updateDistance(getDistanceInMeters(steps = currentSteps))
                pedometerRepository.updateTotalExerciseDuration(getTotalExerciseDuration())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onDestroy() {
        serviceScope.cancel()
        sensorManager?.unregisterListener(this)
        Log.d(TAG, "resetData: \n totalSteps: $totalSteps \n previousTotalSteps: $previousTotalSteps ")
        Log.i(TAG, "Service destroyed")

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    private suspend fun updatePedometerDataInDB(currentSteps: Int, caloriesBurned: Double) {
        // add distance, duration later
        pedometerData.steps = currentSteps
        pedometerData.goal = settingRepository.getPedometerSettings()?.stepGoal ?: 0
        pedometerData.caloriesBurned = caloriesBurned
        pedometerData.endTime = System.currentTimeMillis()
        pedometerData.totalExerciseDuration = getTotalExerciseDuration()
        pedometerData.distanceMeters = getDistanceInMeters(steps = currentSteps)
        pedometerRepository.updatePedometerDataInDB(pedometerData)
    }

    private fun getTotalExerciseDuration(): Long {
        return (pedometerData.endTime - pedometerData.startTime)
    }

    private suspend fun getDistanceInMeters(steps: Int): Double {
        val personalInformation = settingRepository.getPersonalInformation()
        val height = personalInformation?.height
        if (!height.isNullOrBlank()) {
            Log.d(TAG, "getDistanceInMeters: personal data height: $height")
            val heightDouble = height.toDoubleOrNull()
            if (heightDouble != null) {
                return steps * ((heightDouble * 0.41) / 100)
            } else {
                Log.e(TAG, "getDistanceInMeters: invalid height format")
            }
        } else {
            Log.d(TAG, "getDistanceInMeters: no height data")
        }
        return steps * defaultStepLength
    }

    private fun loadData() {
//        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
//        previousTotalSteps = sharedPreferences.getFloat("key1", 0f)

        previousTotalSteps = pedometerRepository.getSteps()
        Log.i(TAG, "Loaded previous steps: $previousTotalSteps")
    }
}
