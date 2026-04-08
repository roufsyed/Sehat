package com.rouf.saht.meditation

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.rouf.saht.common.model.MeditationQuote
import com.rouf.saht.common.model.MeditationQuotes
import com.rouf.saht.common.model.Sound
import com.rouf.saht.databinding.FragmentMeditationBinding
import io.paperdb.Paper
import kotlin.collections.emptyList

class MeditationFragment : Fragment() {

    private val TAG = MeditationFragment::class.java.simpleName

    private var _binding: FragmentMeditationBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MeditationSoundAdapter
    private var currentPlayingFile: String? = null
    private var countdownTimer: CountDownTimer? = null

    // ---- Built-in sounds bundled as app assets ----
    private val builtInSounds: List<Sound> = listOf(
        Sound("Rain",          "rain.mp3",        180, "Nature", "#4DB6AC"),
        Sound("Water Waves",   "water_waves.mp3", 240, "Nature", "#64B5F6"),
        Sound("Lightning",     "lightning.mp3",    60, "Nature", "#FFF176"),
        Sound("Bird Chirping", "birds.mp3",        200, "Nature", "#81C784"),
        Sound("Wind Breeze",   "wind_breeze.mp3",  150, "Nature", "#E57373"),
        Sound("Forest",        "forest.mp3",       150, "Nature", "#A1887F")
    )

    // Palette for custom sound cards
    private val customSoundColors = listOf(
        "#FF7043", "#AB47BC", "#26A69A", "#7E57C2", "#EC407A", "#5C6BC0"
    )

    // ---- SAF launcher: pick an audio file from device storage ----
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> onAudioFilePicked(uri) }
        }
    }

    /**
     * Receives ACTION_PLAYBACK_STOPPED from MeditationService when the countdown
     * ends or the user taps Stop in the notification.
     */
    private val playbackStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Playback stopped broadcast received")
            currentPlayingFile = null
            adapter.updatePlayingSound(null)
            stopTimer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeditationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MeditationSoundAdapter(
            soundList           = allSounds(),
            onSoundClick        = { sound -> togglePlayback(sound) },
            onAddClick          = { launchAudioPicker() },
            onDeleteCustomSound = { sound -> showDeleteConfirmation(sound) }
        )

        binding.rlMeditationPlayback.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@MeditationFragment.adapter
        }

        initChipListeners()
        initBreathingCards()
        initRandomMeditationQuote()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            playbackStoppedReceiver,
            IntentFilter(MeditationService.ACTION_PLAYBACK_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Restore playing state if the service is still running (e.g. user navigated away and back)
        val playing = MeditationService.currentSoundFile
        if (playing != null && currentPlayingFile != playing) {
            currentPlayingFile = playing
            adapter.updatePlayingSound(playing)
            val remaining = MeditationService.endTimeMs - System.currentTimeMillis()
            if (remaining > 0) startTimer(remaining)
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(playbackStoppedReceiver)
        // Service keeps running in the background — do NOT stop it here.
    }

    override fun onDestroyView() {
        countdownTimer?.cancel()
        countdownTimer = null
        super.onDestroyView()
        _binding = null
    }

    // ---- Timer ----

    private fun startTimer(durationMs: Long) {
        countdownTimer?.cancel()
        _binding?.cardTimer?.let { card ->
            card.alpha = 0f
            card.translationY = -40f
            card.visibility = View.VISIBLE
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        countdownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = (millisUntilFinished / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                _binding?.tvTimer?.text = String.format("%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                stopTimer()
            }
        }.start()
    }

    private fun stopTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
        _binding?.cardTimer?.let { card ->
            card.animate()
                .alpha(0f)
                .translationY(-40f)
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { card.visibility = View.GONE }
                .start()
        }
    }

    // ---- Duration ----

    private fun initChipListeners() {
        binding.chip4000.setOnClickListener  { binding.etStepGoal.setText("5")  }
        binding.chip10000.setOnClickListener { binding.etStepGoal.setText("10") }
        binding.chip12000.setOnClickListener { binding.etStepGoal.setText("15") }
        binding.chip15000.setOnClickListener { binding.etStepGoal.setText("20") }
    }

    /** Reads the duration input (minutes), clamps to 1–120, returns milliseconds. */
    private fun getDurationMs(): Long {
        val minutes = binding.etStepGoal.text?.toString()?.toLongOrNull() ?: DEFAULT_DURATION_MIN
        return minutes.coerceIn(1L, 120L) * 60L * 1000L
    }

    // ---- Playback ----

    private fun togglePlayback(sound: Sound) {
        if (currentPlayingFile == sound.file) {
            sendStopIntent()
            currentPlayingFile = null
            adapter.updatePlayingSound(null)
            stopTimer()
        } else {
            sendPlayIntent(sound)
            currentPlayingFile = sound.file
            adapter.updatePlayingSound(sound.file)
            startTimer(getDurationMs())
        }
    }

    private fun sendPlayIntent(sound: Sound) {
        val intent = Intent(requireContext(), MeditationService::class.java).apply {
            action = MeditationService.ACTION_PLAY
            putExtra(MeditationService.EXTRA_SOUND_FILE, sound.file)
            putExtra(MeditationService.EXTRA_SOUND_NAME, sound.name)
            putExtra(MeditationService.EXTRA_DURATION_MS, getDurationMs())
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun sendStopIntent() {
        requireContext().startService(
            Intent(requireContext(), MeditationService::class.java).apply {
                action = MeditationService.ACTION_STOP
            }
        )
    }

    // ---- Custom sounds ----

    private fun launchAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        pickAudioLauncher.launch(intent)
    }

    private fun onAudioFilePicked(uri: Uri) {
        // Take a persistable read permission so we can replay the file in future sessions
        requireContext().contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        showNameInputDialog(uri)
    }

    private fun showNameInputDialog(uri: Uri) {
        val input = EditText(requireContext()).apply {
            hint = "e.g. Ocean Waves"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Name this sound")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) saveCustomSound(name, uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCustomSound(name: String, uri: Uri) {
        val existing = loadCustomSounds().toMutableList()
        val color = customSoundColors[existing.size % customSoundColors.size]
        existing.add(Sound(name = name, file = uri.toString(), duration = 0, category = "Custom", backgroundColor = color))
        Paper.book().write(KEY_CUSTOM_SOUNDS, existing)
        adapter.updateSounds(allSounds())
    }

    private fun showDeleteConfirmation(sound: Sound) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete '${sound.name}'?")
            .setMessage("This sound will be removed from your meditation sounds.")
            .setPositiveButton("Delete") { _, _ -> deleteCustomSound(sound) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCustomSound(sound: Sound) {
        if (currentPlayingFile == sound.file) {
            sendStopIntent()
            currentPlayingFile = null
        }
        val updated = loadCustomSounds().filter { it.file != sound.file }
        Paper.book().write(KEY_CUSTOM_SOUNDS, updated)
        adapter.updateSounds(allSounds())
    }

    private fun loadCustomSounds(): List<Sound> =
        Paper.book().read<List<Sound>>(KEY_CUSTOM_SOUNDS, emptyList<Sound>()) ?: emptyList()

    private fun allSounds(): List<Sound> = builtInSounds + loadCustomSounds()

    // ---- Breathing exercises ----

    private fun initBreathingCards() {
        binding.cardBreathing478.setOnClickListener {
            startActivity(Intent(requireContext(), BreathingExerciseActivity::class.java).apply {
                putExtra(BreathingExerciseActivity.EXTRA_PATTERN, BreathingExerciseActivity.PATTERN_478)
            })
        }
        binding.cardBreathingBox.setOnClickListener {
            startActivity(Intent(requireContext(), BreathingExerciseActivity::class.java).apply {
                putExtra(BreathingExerciseActivity.EXTRA_PATTERN, BreathingExerciseActivity.PATTERN_BOX)
            })
        }
    }

    // ---- Misc ----

    private fun initRandomMeditationQuote() {
        val quote: MeditationQuote = MeditationQuotes.getRandomQuote()
        binding.tvMotivationalQuote.text = "${quote.quote}\n— ${quote.author}"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MeditationService.CHANNEL_ID,
                "Meditation Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val KEY_CUSTOM_SOUNDS   = "custom_meditation_sounds"
        private const val DEFAULT_DURATION_MIN = 15L
    }
}
