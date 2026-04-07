package com.rouf.saht.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.rouf.saht.databinding.FragmentOnboardingPermissionsBinding

class OnboardingPermissionsFragment : Fragment(), OnboardingPageFragment {

    private var _binding: FragmentOnboardingPermissionsBinding? = null
    private val binding get() = _binding!!

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateButtonVisibility()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animateEntrance()
        updateButtonVisibility()

        binding.btnGrantPermissions.setOnClickListener {
            val permissions = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updateButtonVisibility() {
        _binding?.btnGrantPermissions?.visibility =
            if (allPermissionsGranted()) View.GONE else View.VISIBLE
    }

    override fun onNextTapped(proceed: () -> Unit) {
        proceed()
    }

    private fun animateEntrance() {
        val header = listOf(binding.tvHeading, binding.tvSubtitle)
        header.forEach { it.alpha = 0f; it.translationY = 30f }
        header.forEachIndexed { i, v ->
            v.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(i * 150L).start()
        }

        val rows = listOf(binding.rowStepCounter, binding.rowNotifications, binding.rowCamera)
        rows.forEach { it.alpha = 0f; it.translationX = -80f }
        rows.forEachIndexed { i, v ->
            v.animate().alpha(1f).translationX(0f).setDuration(450).setStartDelay(200L + i * 120L).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
