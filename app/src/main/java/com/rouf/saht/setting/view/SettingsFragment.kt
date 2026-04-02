package com.rouf.saht.setting.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.rouf.saht.R
import com.rouf.saht.common.helper.BackupUtils
import com.rouf.saht.databinding.FragmentSettingsBinding
import com.rouf.saht.setting.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
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

        return root
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            settingsViewModel.getPersonalInformation()
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

        binding.llData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Data")
                .setItems(arrayOf("Export data", "Import data")) { _, which ->
                    if (which == 0) {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, "sehat_backup.json")
                        }
                        exportLauncher.launch(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                        }
                        importLauncher.launch(intent)
                    }
                }
                .show()
        }
    }

    fun requestStoragePermissions(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
