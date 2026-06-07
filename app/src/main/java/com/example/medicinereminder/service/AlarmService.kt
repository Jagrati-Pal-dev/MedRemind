package com.example.medicinereminder.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.medicinereminder.R
import com.example.medicinereminder.util.NotificationHelper
import com.example.medicinereminder.ui.AlarmActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val medicineId = intent?.getIntExtra("MEDICINE_ID", -1) ?: -1
        val medicineName = intent?.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val dosage = intent?.getStringExtra("DOSAGE") ?: ""

        Log.d("AlarmService", "onStartCommand for $medicineName")

        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("DOSAGE", dosage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(medicineName)
            .setContentText("Time to take your $dosage")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
        
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        // Launch Activity
        startActivity(activityIntent)

        // Play Sound with Fallback
        try {
            if (mediaPlayer == null) {
                // Try custom sound, if fails use default alarm tone
                mediaPlayer = try {
                    MediaPlayer.create(this, R.raw.medicine_reminder) ?: createDefaultPlayer()
                } catch (e: Exception) {
                    createDefaultPlayer()
                }
                
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to play sound", e)
        }

        return START_STICKY
    }

    private fun createDefaultPlayer(): MediaPlayer? {
        val alert: Uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
        
        return MediaPlayer().apply {
            setDataSource(applicationContext, alert)
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            prepare()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}
