package com.rouf.saht.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.rouf.saht.common.model.MeditationQuote
import com.rouf.saht.common.model.MeditationQuotes
import com.rouf.saht.common.model.Sound
import com.rouf.saht.databinding.FragmentMeditationBinding

class MeditationFragment : Fragment() {

    private var isPlaying = false
    private var playbackDuration = 10000L
    private var _binding: FragmentMeditationBinding? = null
    private val binding get() = _binding!!

    private var currentPlayingSound: com.rouf.saht.common.model.Sound? = null
//    private val soundList: List<Sound> = listOf(
//        Sound("Rain", "rain.mp3", 180, "Nature", "#76B5C5"),  // Light Blue
//        Sound("Water Waves", "water_waves.mp3", 240, "Nature", "#5A9BD4"),  // Blue
//        Sound("Lightning", "lightning.mp3", 60, "Nature", "#F4D03F"),  // Yellow
//        Sound("Bird Chirping", "birds.mp3", 200, "Nature", "#A3CB38"),  // Green
//        Sound("Wind Breeze", "wind_breeze.mp3", 150, "Nature", "#C5E1A5"),  // Light Green
//        Sound("Forest", "forest.mp3", 150, "Nature", "#2E7D32")  // Dark Green
//    )

    private val soundList: List<Sound> = listOf(
        Sound("Rain", "rain.mp3", 180, "Nature", "#4DB6AC"),  // Teal
        Sound("Water Waves", "water_waves.mp3", 240, "Nature", "#64B5F6"),  // Blue
        Sound("Lightning", "lightning.mp3", 60, "Nature", "#FFF176"),  // Yellow
        Sound("Bird Chirping", "birds.mp3", 200, "Nature", "#81C784"),  // Green
        Sound("Wind Breeze", "wind_breeze.mp3", 150, "Nature", "#E57373"),  // Red
        Sound("Forest", "forest.mp3", 150, "Nature", "#A1887F")  // Brown
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
    }

    private fun initRandomMeditationQuote() {
        val meditationQuote: MeditationQuote = MeditationQuotes.getRandomQuote()
        binding.tvMotivationalQuote.text = "${meditationQuote.quote}\n- ${meditationQuote.author}"
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

        val adapter = MeditationSoundAdapter(soundList) { sound ->
            // Handle sound click event (e.g., play the sound)
            togglePlayback(sound)
        }

        binding.rlMeditationPlayback.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
        }

        initRandomMeditationQuote()
    }

    private fun handleSoundClick(sound: Sound) {
        // Example: Show a toast or play sound
        Toast.makeText(requireContext(), "Clicked: ${sound.name}", Toast.LENGTH_SHORT).show()

        // TODO: Implement logic to play sound using ExoPlayer or MediaPlayer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun togglePlayback(sound: Sound) {
        if (currentPlayingSound == sound && isPlaying) {
            stopMeditationService()
            isPlaying = false
            currentPlayingSound = null
        } else {
            startMeditationService(sound)
            isPlaying = true
            currentPlayingSound = sound
        }
    }

    private fun startMeditationService(sound: Sound) {
        val intent = Intent(requireContext(), MeditationService::class.java).apply {
            action = MeditationService.ACTION_PLAY
            putExtra("SOUND_NAME", sound.name)
            putExtra("SOUND_FILE", sound.file)
            putExtra("DURATION", playbackDuration)
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopMeditationService() {
        val intent = Intent(requireContext(), MeditationService::class.java).apply {
            action = MeditationService.ACTION_STOP
        }
        requireContext().startService(intent)
    }
}
