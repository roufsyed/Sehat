package com.rouf.saht.pedometer.view

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.rouf.saht.common.activity.BaseActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rouf.saht.R
import com.rouf.saht.common.helper.TimeUtil
import com.rouf.saht.common.model.PedometerData
import com.rouf.saht.databinding.ActivityPedometerDetailBinding
import com.rouf.saht.databinding.DialogConfirmationBinding
import com.rouf.saht.heartRate.view.HeartRateDetailActivity
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PedometerDetailActivity : BaseActivity() {
    private val TAG: String = HeartRateDetailActivity::class.java.simpleName
    private lateinit var binding: ActivityPedometerDetailBinding
    private lateinit var pedometerViewModel: PedometerViewModel
    private var pedometerData: PedometerData? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPedometerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedometerViewModel = ViewModelProvider(this@PedometerDetailActivity)[PedometerViewModel::class.java]

        pedometerData = intent.getSerializableExtra("pedometerData") as? PedometerData
        pedometerData?.let { initView(it) }

        onClick()

    }

    private fun onClick() {
        binding.btnDelete.setOnClickListener {
            showConfirmationDialog(this@PedometerDetailActivity)
        }
    }

    private fun showConfirmationDialog(context: Context) {
        val dialogBinding = DialogConfirmationBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, R.style.DialogThemeSize)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        val window = dialog.window ?: return
        val wlp = window.attributes
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT
        wlp.height = WindowManager.LayoutParams.WRAP_CONTENT
        wlp.gravity = Gravity.BOTTOM
        wlp.windowAnimations = R.style.bottomSheetAnimation
        window.attributes = wlp

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            Log.d(TAG, "showConfirmationDialog: Confirmed")
            lifecycleScope.launch {
                pedometerData?.let { data ->
                    val isDeleted: Boolean = pedometerViewModel.deletePedometerDataByTimestamp(data.timestamp)
                    if (isDeleted) finish()
                    else
                        Log.e(TAG, "showConfirmationDialog: Failed to delete")
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initView(pedometerData: PedometerData) {
        binding.dateValue.text = TimeUtil.timestampToDateTime(pedometerData.timestamp)
        binding.stepsValue.text = "${pedometerData.steps} steps"
        binding.goalValue.text = "${pedometerData.goal} steps"
        binding.caloriesBurnedValue.text = "${pedometerData.caloriesBurned} kcal"
        binding.distanceValue.text = "${pedometerData.distanceMeters} meters"
        binding.totalExerciseDurationValue.text = "${pedometerData.totalExerciseDuration / 1000} sec"
        binding.startTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(pedometerData.startTime))
        binding.endTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(pedometerData.endTime))
    }
}