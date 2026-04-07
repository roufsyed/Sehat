package com.rouf.saht.setting.view

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rouf.saht.common.activity.BaseActivity
import com.google.android.material.textfield.TextInputEditText
import com.rouf.saht.R
import com.rouf.saht.common.model.Gender
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.databinding.ActivityPersonalInformationBinding
import com.rouf.saht.pedometer.viewModel.PedometerViewModel
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class PersonalInformationActivity : BaseActivity() {

    private val TAG: String = PersonalInformationActivity::class.java.simpleName
    private lateinit var binding: ActivityPersonalInformationBinding
    private lateinit var pedometerViewModel: PedometerViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private lateinit var personalInformation: PersonalInformation
    private lateinit var oldGender: Gender
    private lateinit var oldFormatedDate: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPersonalInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pedometerViewModel = ViewModelProvider(this@PersonalInformationActivity)[PedometerViewModel::class.java]
        settingsViewModel = ViewModelProvider(this@PersonalInformationActivity)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            personalInformation = settingsViewModel.getPersonalInformation()

            oldGender = personalInformation.gender
            oldFormatedDate = personalInformation.formatedDate

            initView()
            onClick()
        }
    }

    private fun initView() {
        val name = personalInformation.name
        val gender = personalInformation.gender
        val height = personalInformation.height
        val weight = personalInformation.weight
        val dateOfBirth: String = personalInformation.formatedDate
        val age: String = personalInformation.age

        Log.d(TAG, "personalInformation: $personalInformation")

        binding.etName.setText(name)
        setGenderView(gender)
        binding.etHeight.setText(height)
        binding.etWeight.setText(weight)
        binding.etDateOfBirth.setText(dateOfBirth)
        binding.tvAgeResult.text = "Your age is $age"
    }

    private fun onClick() {
        val simpleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etName.addTextChangedListener(simpleWatcher)
        binding.etHeight.addTextChangedListener(simpleWatcher)
        binding.etWeight.addTextChangedListener(simpleWatcher)

        binding.btnSave.setOnClickListener {
            personalInformation.name = binding.etName.text.toString().trim()
            personalInformation.height = binding.etHeight.text.toString().trim()
            personalInformation.weight = binding.etWeight.text.toString().trim()

            lifecycleScope.launch {
                settingsViewModel.savePersonalInformation(personalInformation)
            }

            oldGender = personalInformation.gender
            oldFormatedDate = personalInformation.formatedDate
            binding.btnSave.isEnabled = false

            hideKeyboard()

            lifecycleScope.launch(Dispatchers.Main) {
                showSuccessAnimation()
            }
        }

        binding.etDateOfBirth.setOnClickListener {
            showDatePickerDialog(binding.etDateOfBirth, binding.tvAgeResult)
        }

        binding.containerMale.setOnClickListener {
            setGenderView(Gender.MALE)
            personalInformation.gender = Gender.MALE
            updateSaveButtonState()
        }

        binding.containerFemale.setOnClickListener {
            setGenderView(Gender.FEMALE)
            personalInformation.gender = Gender.FEMALE
            updateSaveButtonState()
        }
    }

    private fun updateSaveButtonState() {
        val nameChanged = binding.etName.text.toString().trim() != personalInformation.name
        val heightChanged = binding.etHeight.text.toString().trim() != personalInformation.height
        val weightChanged = binding.etWeight.text.toString().trim() != personalInformation.weight
        val genderChanged = personalInformation.gender != oldGender
        val dateChanged = personalInformation.formatedDate != oldFormatedDate

        val nameValid = binding.etName.text.toString().trim().isNotEmpty()
        val heightValid = binding.etHeight.text.toString().trim().toDoubleOrNull()?.let { it > 0 } ?: false
        val weightValid = binding.etWeight.text.toString().trim().toDoubleOrNull()?.let { it > 0 } ?: false

        val hasChange = nameChanged || heightChanged || weightChanged || genderChanged || dateChanged
        binding.btnSave.isEnabled = hasChange && nameValid && heightValid && weightValid
    }

    private fun setGenderView(type: Gender) {
        val primaryColor = ColorStateList.valueOf(BaseActivity.effectivePrimary(this))
        val transparentColor = ColorStateList.valueOf(getColor(R.color.transparent))

        when (type.value) {
            0 -> {
                binding.wrapperMale.backgroundTintList = primaryColor
                binding.wrapperFemale.backgroundTintList = transparentColor
            }
            1 -> {
                binding.wrapperFemale.backgroundTintList = primaryColor
                binding.wrapperMale.backgroundTintList = transparentColor
            }
        }
    }

    private fun showDatePickerDialog(etDate: TextInputEditText, tvAgeResult: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val context = ContextThemeWrapper(this, R.style.CustomDatePickerStyle)

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = formatSelectedDate(selectedYear, selectedMonth, selectedDay)

                etDate.setText(formattedDate)

                val age = calculateAge(selectedYear, selectedMonth, selectedDay)
                tvAgeResult.text = "Your age is $age"

                personalInformation.formatedDate = formattedDate
                personalInformation.selectedDay = selectedDay
                personalInformation.selectedMonth = selectedMonth
                personalInformation.selectedYear = selectedYear
                personalInformation.age = age.toString()
                updateSaveButtonState()
            },
            year,
            month,
            day
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun formatSelectedDate(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance().apply {
            set(year, month, day)
        }

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    private fun showSuccessAnimation() {
        val ivSuccess = binding.ivSuccess
        ivSuccess.visibility = View.VISIBLE

        Glide.with(this)
            .load(R.drawable.gif_success)
            .into(ivSuccess)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).clear(ivSuccess)
                ivSuccess.visibility = View.GONE
            }
        }, 2200)
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}