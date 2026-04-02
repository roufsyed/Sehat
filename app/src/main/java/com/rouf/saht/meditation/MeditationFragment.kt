package com.rouf.saht.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.rouf.saht.common.model.MeditationQuote
import com.rouf.saht.common.model.MeditationQuotes
import com.rouf.saht.common.model.Sound
import com.rouf.saht.databinding.FragmentMeditationBinding

class MeditationFragment : Fragment() {

    private val TAG = MeditationFragment::class.java.simpleName

    private var _binding: FragmentMeditationBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MeditationSoundAdapter
    private var currentPlayingFile: String? = null

    private val soundList: List<Sound> = listOf(
        Sound("Rain",         "rain.mp3",         180, "Nature", "#4DB6AC"),
        Sound("Water Waves",  "water_waves.mp3",   240, "Nature", "#64B5F6"),
        Sound("Lightning",    "lightning.mp3",      60, "Nature", "#FFF176"),
        Sound("Bird Chirping","birds.mp3",          200, "Nature", "#81C784"),
        Sound("Wind Breeze",  "wind_breeze.mp3",   150, "Nature", "#E57373"),
        Sound("Forest",       "forest.mp3",         150, "Nature", "#A1887F")
    )

    /**
     * Receives ACTION_PLAYBACK_STOPPED from MeditationService when the countdown
     * ends or the user taps Stop in the notification.
     */
    private val playbackStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received playback stopped broadcast")
            currentPlayingFile = null
            adapter.updatePlayingSound(null)
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

        adapter = MeditationSoundAdapter(soundList) { sound -> togglePlayback(sound) }

        binding.rlMeditationPlayback.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@MeditationFragment.adapter
        }

        initChipListeners()
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
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(playbackStoppedReceiver)
        // Stop service when the user navigates away so it doesn't play silently in background
        if (currentPlayingFile != null) {
            sendStopIntent()
            currentPlayingFile = null
            adapter.updatePlayingSound(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initChipListeners() {
        binding.chip4000.setOnClickListener { binding.etStepGoal.setText("5") }
        binding.chip10000.setOnClickListener { binding.etStepGoal.setText("10") }
        binding.chip12000.setOnClickListener { binding.etStepGoal.setText("15") }
        binding.chip15000.setOnClickListener { binding.etStepGoal.setText("20") }
    }

    private fun initRandomMeditationQuote() {
        val quote: MeditationQuote = MeditationQuotes.getRandomQuote()
        binding.tvMotivationalQuote.text = "${quote.quote}\n— ${quote.author}"
    }

    private fun togglePlayback(sound: Sound) {
        if (currentPlayingFile == sound.file) {
            // Tapping the currently playing sound stops it
            sendStopIntent()
            currentPlayingFile = null
            adapter.updatePlayingSound(null)
        } else {
            // Start (or switch to) the new sound
            sendPlayIntent(sound)
            currentPlayingFile = sound.file
            adapter.updatePlayingSound(sound.file)
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
        val intent = Intent(requireContext(), MeditationService::class.java).apply {
            action = MeditationService.ACTION_STOP
        }
        requireContext().startService(intent)
    }

    /**
     * Reads the user-entered duration (in minutes) from the input field,
     * validates it, and converts to milliseconds.
     */
    private fun getDurationMs(): Long {
        val minutes = binding.etStepGoal.text?.toString()?.toLongOrNull() ?: DEFAULT_DURATION_MIN
        val clamped = minutes.coerceIn(1L, 120L)
        return clamped * 60L * 1000L
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MeditationService.CHANNEL_ID,
                "Meditation Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val DEFAULT_DURATION_MIN = 15L
    }
}
