package com.example.medicinereminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.R
import com.example.medicinereminder.ui.MainActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel_v4" // Upgraded to v4
        const val CHANNEL_NAME = "Medicine Alarms"
    }

    fun showNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for Medicine Reminders"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
