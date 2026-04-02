package com.rouf.saht.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rouf.saht.databinding.FragmentOnboardingGoalBinding
import com.rouf.saht.setting.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingGoalFragment : Fragment(), OnboardingPageFragment {

    private var _binding: FragmentOnboardingGoalBinding? = null
    private val binding get() = _binding!!

    private val settingsViewModel by lazy {
        ViewModelProvider(requireActivity())[SettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etStepGoal.setText("10000")

        binding.chip5000.setOnClickListener {
            binding.etStepGoal.setText(binding.chip5000.text.toString().replace(",", ""))
        }
        binding.chip8000.setOnClickListener {
            binding.etStepGoal.setText(binding.chip8000.text.toString().replace(",", ""))
        }
        binding.chip10000.setOnClickListener {
            binding.etStepGoal.setText(binding.chip10000.text.toString().replace(",", ""))
        }
        binding.chip12000.setOnClickListener {
            binding.etStepGoal.setText(binding.chip12000.text.toString().replace(",", ""))
        }
    }

    override fun onNextTapped(proceed: () -> Unit) {
        val goalText = binding.etStepGoal.text?.toString()?.trim() ?: ""
        val goal = goalText.toIntOrNull() ?: 10000

        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsViewModel.getPedometerSettings()
            settings.stepGoal = goal
            settingsViewModel.savePedometerSettings(settings)
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
