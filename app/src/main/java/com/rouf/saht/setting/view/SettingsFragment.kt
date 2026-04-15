package com.rouf.saht.setting.view

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.rouf.saht.common.activity.BaseActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rouf.saht.BuildConfig
import com.rouf.saht.R
import com.rouf.saht.common.helper.BackupUtils
import com.rouf.saht.common.helper.DebugDataSeeder
import com.rouf.saht.databinding.FragmentSettingsBinding
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.paperdb.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val TAG = SettingsFragment::class.java.simpleName
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsViewModel: SettingsViewModel

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = BackupUtils.exportData(requireContext(), uri)
                Toast.makeText(
                    requireContext(),
                    if (success) "Data exported successfully" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = BackupUtils.importData(requireContext(), uri)
                Toast.makeText(
                    requireContext(),
                    if (success) "Data imported successfully" else "Import failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val customizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            requireActivity().recreate()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        settingsViewModel = ViewModelProvider(this@SettingsFragment)[SettingsViewModel::class.java]

        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
        }

        observer()
        onClick()
        setupCollapsingToolbar()

        return root
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
        }
    }

    private var originalStatusBarColor = 0

    private fun setupCollapsingToolbar() {
        val window = requireActivity().window
        originalStatusBarColor = window.statusBarColor

        val primaryDark = BaseActivity.effectivePrimaryDark(requireContext())
        val primary = BaseActivity.effectivePrimary(requireContext())

        // Status bar matches the gradient start color (dark background → white icons)
        window.statusBarColor = primaryDark
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        // Update the AppBarLayout gradient background
        val appBarBg = binding.appBar.background
        if (appBarBg is GradientDrawable) {
            appBarBg.colors = intArrayOf(primaryDark, primary)
        }

        // Update the expanded header gradient
        val headerBg = binding.clUserView.background
        if (headerBg is GradientDrawable) {
            headerBg.colors = intArrayOf(primaryDark, primary)
        }

        // Update the content scrim for collapsed state
        val scrim = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(primaryDark, primary)
        )
        binding.collapsingToolbar.contentScrim = scrim

        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalRange = appBarLayout.totalScrollRange
            if (totalRange == 0) return@OnOffsetChangedListener
            val fraction = (-verticalOffset).toFloat() / totalRange
            binding.llCollapsedHeader.alpha = fraction
            binding.clUserView.alpha = 1f - fraction
        })
    }

    private fun observer() {
        settingsViewModel.personalInformation.observe(viewLifecycleOwner) { personalInformation ->
            binding.tvUser.text = personalInformation.name
            binding.tvCollapsedName.text = personalInformation.name
            val avatarRes = when (personalInformation.gender.value) {
                1 -> R.drawable.ic_person_woman
                else -> R.drawable.ic_person_man
            }
            binding.ivProfileImage.setImageResource(avatarRes)
            binding.ivCollapsedAvatar.setImageResource(avatarRes)
        }
    }

    private fun onClick() {
        binding.llDashboardSettings.setOnClickListener {
            startActivity(Intent(activity, DashboardSettingsActivity::class.java))
        }

        binding.llCustomization.setOnClickListener {
            customizationLauncher.launch(Intent(activity, CustomizationActivity::class.java))
        }

        binding.llPersonalInformation.setOnClickListener {
            startActivity(Intent(activity, PersonalInformationActivity::class.java))
        }

        binding.llPedometerSettings.setOnClickListener {
            startActivity(Intent(activity, PedometerSettingsActivity::class.java))
        }

        binding.llHrmSettings.setOnClickListener {
            startActivity(Intent(activity, HeartRateMonitorSettingsActivity::class.java))
        }

        binding.llNotification.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                requireContext().startActivity(intent)
            }
        }

        binding.llData.setOnClickListener {
            val items = arrayOf("Export data", "Import data")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Data")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                                putExtra(Intent.EXTRA_TITLE, "sehat_backup.json")
                            }
                            exportLauncher.launch(intent)
                        }
                        1 -> {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                            }
                            importLauncher.launch(intent)
                        }
                    }
                }
                .show()
        }

        if (BuildConfig.DEBUG) {
            binding.cardSeedData.visibility = View.VISIBLE
            binding.llSeedData.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    DebugDataSeeder.seedAllData()
                }
                Toast.makeText(requireContext(), "Demo data seeded", Toast.LENGTH_SHORT).show()
            }
        }

        binding.llFaq.setOnClickListener {
            startActivity(Intent(activity, FaqActivity::class.java))
        }

        binding.llPrivacyPolicy.setOnClickListener {
            startActivity(Intent(activity, PrivacyPolicyActivity::class.java))
        }
    }

    companion object {
        const val PREF_DARK_MODE       = "pref_dark_mode"
        const val PREF_DOUBLE_TAP_LOCK = "pref_double_tap_lock"
    }

    override fun onDestroyView() {
        val window = requireActivity().window
        window.statusBarColor = originalStatusBarColor
        val isNight = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isNight
        super.onDestroyView()
        _binding = null
    }
}
