package com.rouf.saht.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.rouf.saht.common.activity.BaseActivity
import com.rouf.saht.R
import com.rouf.saht.common.model.Gender
import com.rouf.saht.common.model.PersonalInformation
import com.rouf.saht.databinding.FragmentOnboardingProfileBinding
import com.rouf.saht.setting.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingProfileFragment : Fragment(), OnboardingPageFragment {

    private var _binding: FragmentOnboardingProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedGender = Gender.MALE

    private val settingsViewModel by lazy {
        ViewModelProvider(requireActivity())[SettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animateEntrance()
        setGenderSelected(Gender.MALE)

        binding.cardMale.setOnClickListener {
            selectedGender = Gender.MALE
            setGenderSelected(Gender.MALE)
        }

        binding.cardFemale.setOnClickListener {
            selectedGender = Gender.FEMALE
            setGenderSelected(Gender.FEMALE)
        }
    }

    private fun animateEntrance() {
        val header = listOf(binding.tvHeading, binding.tvSubtitle)
        header.forEach { it.alpha = 0f; it.translationY = 30f }
        header.forEachIndexed { i, v ->
            v.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(i * 150L).start()
        }

        val cards = listOf(binding.cardMale, binding.cardFemale)
        cards.forEach { it.alpha = 0f; it.scaleX = 0.8f; it.scaleY = 0.8f }
        cards.forEachIndexed { i, v ->
            v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(300L + i * 150L).start()
        }
    }

    private fun setGenderSelected(gender: Gender) {
        val selectedColor = BaseActivity.effectivePrimary(requireContext())
        val defaultColor = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorSurfaceVariant,
            requireContext().getColor(R.color.light_grey)
        )

        if (gender == Gender.MALE) {
            binding.cardMale.setCardBackgroundColor(selectedColor)
            binding.cardFemale.setCardBackgroundColor(defaultColor)
        } else {
            binding.cardMale.setCardBackgroundColor(defaultColor)
            binding.cardFemale.setCardBackgroundColor(selectedColor)
        }
    }

    override fun onNextTapped(proceed: () -> Unit) {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            proceed()
            return
        }

        val pi: PersonalInformation = settingsViewModel.getEmptyPersonalInformation().copy(
            name = name,
            gender = selectedGender,
            height = binding.etHeight.text?.toString()?.trim() ?: "",
            weight = binding.etWeight.text?.toString()?.trim() ?: ""
        )

        lifecycleScope.launch(Dispatchers.IO) {
            settingsViewModel.savePersonalInformation(pi)
            withContext(Dispatchers.Main) {
                proceed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
