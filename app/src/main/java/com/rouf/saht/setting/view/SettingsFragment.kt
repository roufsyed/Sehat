package com.rouf.saht.setting.view

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rouf.saht.R
import com.rouf.saht.common.helper.BackupUtils
import com.rouf.saht.common.helper.DebugDataSeeder
import com.rouf.saht.common.receiver.LockScreenAdmin
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
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val adminActivationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled in onResume */ }

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        settingsViewModel = ViewModelProvider(this@SettingsFragment)[SettingsViewModel::class.java]
        devicePolicyManager = requireContext().getSystemService(Activity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(requireContext(), LockScreenAdmin::class.java)

        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
        }

        val isDarkMode = Paper.book().read(PREF_DARK_MODE, false) ?: false
        binding.switchDarkMode.isChecked = isDarkMode

        observer()
        onClick()

        return root
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
        }

        // Sync double-tap lock switch with actual Device Admin state.
        // If the user granted admin on the activation screen, turn the switch on.
        // If they denied or removed it, turn it off and clear the preference.
        val isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
        binding.switchDoubleTapLock.isChecked = isAdminActive
        if (!isAdminActive) {
            Paper.book().write(PREF_DOUBLE_TAP_LOCK, false)
        }
    }

    private fun observer() {
        settingsViewModel.personalInformation.observe(viewLifecycleOwner){ personalInformation ->
            val name = personalInformation.name
            val gender = personalInformation.gender

            binding.tvUser.text = name

            when(gender.value) {
                0 -> {
                    binding.ivProfileImage.setImageResource(R.drawable.ic_person_man)
                }

                1 -> {
                    binding.ivProfileImage.setImageResource(R.drawable.ic_person_woman)
                }
            }
        }
    }

    private fun onClick() {
        binding.llPersonalInformation.setOnClickListener{
            val intent = Intent(activity, PersonalInformationActivity::class.java)
            startActivity(intent)
        }

        binding.llPedometerSettings.setOnClickListener{
            val intent = Intent(activity, PedometerSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.llHrmSettings.setOnClickListener{
            val intent = Intent(activity, HeartRateMonitorSettingsActivity::class.java)
            startActivity(intent)
        }

        binding.llNotification.setOnClickListener{
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

        binding.llPrivacyPolicy.setOnClickListener {
            startActivity(Intent(activity, PrivacyPolicyActivity::class.java))
        }

        binding.llData.setOnClickListener {
            val items = arrayOf(
                "Export data",
                "Import data",
                "Seed demo heart rate data",
                "Seed demo pedometer data"
            )
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
                        2 -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                DebugDataSeeder.seedHeartRateData()
                            }
                            Toast.makeText(requireContext(), "Demo heart rate data added", Toast.LENGTH_SHORT).show()
                        }
                        3 -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                DebugDataSeeder.seedPedometerData()
                            }
                            Toast.makeText(requireContext(), "Demo pedometer data added", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Paper.book().write(PREF_DARK_MODE, isChecked)
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                       else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        binding.switchDoubleTapLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!devicePolicyManager.isAdminActive(adminComponent)) {
                    // Switch flipped on but permission not yet granted — launch activation screen.
                    // Revert the switch now; onResume will turn it on if the user grants permission.
                    binding.switchDoubleTapLock.isChecked = false
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Required to lock the screen on double-tap.")
                    }
                    adminActivationLauncher.launch(intent)
                } else {
                    Paper.book().write(PREF_DOUBLE_TAP_LOCK, true)
                }
            } else {
                Paper.book().write(PREF_DOUBLE_TAP_LOCK, false)
                if (devicePolicyManager.isAdminActive(adminComponent)) {
                    devicePolicyManager.removeActiveAdmin(adminComponent)
                }
            }
        }
    }

    companion object {
        const val PREF_DARK_MODE = "pref_dark_mode"
        const val PREF_DOUBLE_TAP_LOCK = "pref_double_tap_lock"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
