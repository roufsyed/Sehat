package com.rouf.saht.meditation

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rouf.saht.R
import com.rouf.saht.common.activity.MainActivity
import com.rouf.saht.setting.view.CustomizationActivity

class MeditationService : Service() {

    private val TAG = MeditationService::class.java.simpleName

    private var exoPlayer: ExoPlayer? = null
    private var handler: Handler? = null
    private var currentSoundName: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        startForeground(NOTIFICATION_ID, buildNotification(""))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundFile = intent.getStringExtra(EXTRA_SOUND_FILE)
                val soundName = intent.getStringExtra(EXTRA_SOUND_NAME)
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, DEFAULT_DURATION_MS)

                if (soundFile.isNullOrBlank() || soundName.isNullOrBlank()) {
                    Log.e(TAG, "Missing sound extras — aborting playback")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startPlayback(soundFile, soundName, durationMs)
            }
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(soundFile: String, soundName: String, durationMs: Long) {
        handler?.removeCallbacksAndMessages(null)
        releasePlayer()

        currentSoundName = soundName
        currentSoundFile = soundFile
        endTimeMs = System.currentTimeMillis() + durationMs

        // Asset files use asset:/// scheme; custom sounds from device use their content:// URI directly
        val mediaUri = if (soundFile.startsWith("content://")) {
            Uri.parse(soundFile)
        } else {
            Uri.parse("asset:///$soundFile")
        }

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Playback error for '$soundFile': ${error.message}")
                    stopPlayback()
                }
            })
            prepare()
            play()
        }

        updateNotification(soundName)

        handler?.postDelayed({ stopPlayback() }, durationMs)

        Log.d(TAG, "Started playback: $soundName (${durationMs}ms)")
    }

    private fun stopPlayback() {
        currentSoundFile = null
        endTimeMs = 0L
        releasePlayer()
        handler?.removeCallbacksAndMessages(null)
        broadcastPlaybackStopped()
        stopSelf()
    }

    private fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun broadcastPlaybackStopped() {
        sendBroadcast(Intent(ACTION_PLAYBACK_STOPPED).also { it.setPackage(packageName) })
    }

    private fun updateNotification(soundName: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(soundName))
    }

    private fun buildNotification(soundName: String): Notification {
        // Tapping the notification body opens the app directly on the meditation tab
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, CustomizationActivity.SCREEN_MEDITATION)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MeditationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (soundName.isNotEmpty()) "Playing: $soundName" else "Preparing…"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meditation in Progress")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_pause, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        releasePlayer()
        handler?.removeCallbacksAndMessages(null)
        handler = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "meditation_channel"
        const val NOTIFICATION_ID = 2

        const val ACTION_PLAY = "com.rouf.saht.meditation.ACTION_PLAY"
        const val ACTION_STOP = "com.rouf.saht.meditation.ACTION_STOP"
        const val ACTION_PLAYBACK_STOPPED = "com.rouf.saht.meditation.ACTION_PLAYBACK_STOPPED"

        const val EXTRA_SOUND_FILE = "extra_sound_file"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
        const val EXTRA_DURATION_MS = "extra_duration_ms"

        private const val DEFAULT_DURATION_MS = 15 * 60 * 1000L

        /** Non-null while a sound is playing; null when stopped. */
        var currentSoundFile: String? = null
            private set

        /** Epoch ms when current playback will end; 0 when idle. */
        var endTimeMs: Long = 0L
            private set
    }
}
