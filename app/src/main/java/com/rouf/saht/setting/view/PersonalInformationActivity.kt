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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
class PersonalInformationActivity : AppCompatActivity() {

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
        val oldName = personalInformation.name
        val oldHeight = if (personalInformation.height.isNullOrEmpty()) 0.0 else personalInformation.height.toDouble()
        val oldWeight = if (personalInformation.weight.isNullOrEmpty()) 0.0 else personalInformation.weight.toDouble()

        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val nameText = charSequence.toString().trim()

                if (nameText.isNotEmpty()) {
                    if (nameText.isNotEmpty()) {
                        if (nameText != oldName) {
                            binding.btnSave.isEnabled = true
                        } else {
                            binding.btnSave.isEnabled = false
                        }
                    } else {
                        binding.etName.error = "Please enter a valid name"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    binding.etName.error = "Name cannot be empty"
                    binding.btnSave.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        binding.etHeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val heightText = charSequence.toString().trim()

                if (heightText.isNotEmpty()) {
                    val height = heightText.toDoubleOrNull()
                    if (height != null && height > 0) {
                        if (height != oldHeight) {
                            binding.btnSave.isEnabled = true
                        } else {
                            binding.btnSave.isEnabled = false
                        }
                    } else {
                        binding.etHeight.error = "Please enter a valid height"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    binding.etHeight.error = "Height cannot be empty"
                    binding.btnSave.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val weightText = charSequence.toString().trim()

                if (weightText.isNotEmpty()) {
                    val weight = weightText.toDoubleOrNull()
                    if (weight != null && weight > 0) {
                        if (weight != oldWeight) {
                            binding.btnSave.isEnabled = true
                        } else {
                            binding.btnSave.isEnabled = false
                        }
                    } else {
                        binding.etWeight.error = "Please enter a valid weight"
                        binding.btnSave.isEnabled = false
                    }
                } else {
                    binding.etWeight.error = "Weight cannot be empty"
                    binding.btnSave.isEnabled = false
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val height = binding.etHeight.text.toString()
            val weight = binding.etWeight.text.toString()

            Log.d(TAG, "personalInformation: $personalInformation")

            personalInformation.name = name
            personalInformation.height = height
            personalInformation.weight = weight

            lifecycleScope.launch {
                settingsViewModel.savePersonalInformation(personalInformation)
            }
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

            if (oldGender.value != personalInformation.gender.value) {
                binding.btnSave.isEnabled = true
            } else {
                binding.btnSave.isEnabled = false
            }
        }

        binding.containerFemale.setOnClickListener {
            setGenderView(Gender.FEMALE)
            personalInformation.gender = Gender.FEMALE

            if (oldGender.value != personalInformation.gender.value) {
                binding.btnSave.isEnabled = true
            } else {
                binding.btnSave.isEnabled = false
            }
        }
    }

    private fun setGenderView(type: Gender) {
        val genderType = type.value

        when (genderType) {
            0 -> {
                binding.wrapperMale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_500)
                binding.wrapperFemale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.transparent)

            }

            1 -> {
                binding.wrapperFemale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_500)
                binding.wrapperMale.backgroundTintList = ContextCompat.getColorStateList(this, R.color.transparent)
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

                if (formattedDate != oldFormatedDate) {
                    Log.d(TAG, "showDatePickerDialog: formattedDate: $formattedDate, oldFormatedDate:$oldFormatedDate")
                    personalInformation.formatedDate = formattedDate
                    personalInformation.selectedDay = selectedDay
                    personalInformation.selectedMonth = selectedMonth
                    personalInformation.selectedYear = selectedYear
                    personalInformation.age = age.toString()
                    binding.btnSave.isEnabled = true
                } else {
                    binding.btnSave.isEnabled = false
                }
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