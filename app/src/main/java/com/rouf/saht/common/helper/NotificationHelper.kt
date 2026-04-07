package com.rouf.saht.common.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.rouf.saht.R
import com.rouf.saht.common.activity.MainActivity
import com.rouf.saht.setting.view.CustomizationActivity

@RequiresApi(Build.VERSION_CODES.O)
class NotificationHelper(private val context: Context) {
    init {
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Delete the old channel so the stale "Foreground Service" name is removed
        manager.deleteNotificationChannel("ForegroundServiceChannel")
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shows your live step count and calories while the pedometer is running"
        manager.createNotificationChannel(channel)
    }

    fun getServiceNotification(stepsData: String?, calorieData: String?): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, CustomizationActivity.SCREEN_PEDOMETER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("$stepsData    $calorieData")
            .setSubText("Your Health App")
            .setSmallIcon(R.drawable.ic_heart)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "pedometer_channel"
        private const val CHANNEL_NAME = "Pedometer"
    }
}