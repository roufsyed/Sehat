package com.rouf.saht.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rouf.saht.common.helper.BackupUtils
import com.rouf.saht.databinding.FragmentOnboardingWelcomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingWelcomeFragment : Fragment(), OnboardingPageFragment {

    private var _binding: FragmentOnboardingWelcomeBinding? = null
    private val binding get() = _binding!!
    private var hoverAnimator: AnimatorSet? = null

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handleImport(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startHoverAnimation()
        animateTextEntrance()

        binding.btnImportBackup.setOnClickListener {
            importLauncher.launch("application/json")
        }
    }

    private fun handleImport(uri: Uri) {
        val b = _binding ?: return
        b.btnImportBackup.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                BackupUtils.importData(requireContext(), uri)
            }

            val binding = _binding
            if (binding == null) return@launch  // fragment detached while IO was in flight

            if (success) {
                (requireActivity() as? OnboardingActivity)?.completeOnboarding()
            } else {
                binding.btnImportBackup.isEnabled = true
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Restore Failed")
                    .setMessage(
                        "The selected file could not be read. " +
                        "Please make sure you chose a valid Sehat backup file (.json)."
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun startHoverAnimation() {
        val illustration = binding.ivIllustration

        val floatUp = ObjectAnimator.ofFloat(illustration, View.TRANSLATION_Y, 0f, -24f).apply {
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
        }
        val floatDown = ObjectAnimator.ofFloat(illustration, View.TRANSLATION_Y, -24f, 0f).apply {
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
        }

        hoverAnimator = AnimatorSet().apply {
            playSequentially(floatUp, floatDown)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (_binding != null) start()
                }
            })
            start()
        }
    }

    private fun animateTextEntrance() {
        val views = listOf(
            binding.tvTitle,
            binding.tvSubtitle,
            binding.btnImportBackup,
            binding.tvPrivacyNote
        )
        views.forEach { it.alpha = 0f; it.translationY = 40f }

        views.forEachIndexed { index, v ->
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300L + index * 200L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    override fun onDestroyView() {
        hoverAnimator?.cancel()
        hoverAnimator = null
        super.onDestroyView()
        _binding = null
    }
}
