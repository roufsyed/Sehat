package com.rouf.saht.meditation

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.rouf.saht.R

class MeditationService : Service() {
    private val TAG: String? = MeditationService::class.java.simpleName
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    private var defaultPlaybackDuration = 60000L // 1 min
    private var handler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeExoPlayer()
    }

    private fun initializeExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri("asset:///rain.mp3")
            setMediaItem(mediaItem)
            prepare()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val duration = intent.getLongExtra("DURATION", defaultPlaybackDuration)
                Log.d(TAG, "onStartCommand: Duration: $duration")
                startPlayback(duration)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startPlayback(duration: Long) {
        if (!isPlaying) {
            exoPlayer?.play()
            isPlaying = true
            startCountdown(duration)
        }
    }

    private fun startCountdown(duration: Long) {
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed({
            stopSelf()
        }, duration)
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        handler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, MeditationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meditation in Progress")
            .setContentText("Playing meditation audio")
            .setSmallIcon(R.drawable.ic_play)
            .setOngoing(true)
            .addAction(R.drawable.ic_play, "Stop", stopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "meditation_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "PLAY"
        const val ACTION_STOP = "STOP"
    }
}
