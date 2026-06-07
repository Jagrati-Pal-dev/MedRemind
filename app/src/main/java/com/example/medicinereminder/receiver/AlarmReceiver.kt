package com.example.medicinereminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medicinereminder.service.AlarmService
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync() // Essential for background reliability
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MedicineReminder:AlarmWakeLock")
        wakeLock.acquire(20 * 1000L)

        val medicineId = intent.getIntExtra("MEDICINE_ID", -1)
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val isExpiryAlert = intent.getBooleanExtra("IS_EXPIRY_ALERT", false)
        
        Log.d("AlarmReceiver", "Broadcast received for $medicineName")

        if (isExpiryAlert) {
            val expiryDate = intent.getStringExtra("EXPIRY_DATE") ?: ""
            com.example.medicinereminder.util.NotificationHelper(context)
                .showNotification("Medicine Expiring Soon!", "$medicineName will expire on $expiryDate.")
            pendingResult.finish()
            return
        }

        // Reschedule
        if (medicineId != -1) {
            val prefs = context.getSharedPreferences("medicines", Context.MODE_PRIVATE)
            val json = prefs.getString("medicine_list", null)
            if (json != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<com.example.medicinereminder.model.Medicine>>() {}.type
                val medicines: List<com.example.medicinereminder.model.Medicine> = com.google.gson.Gson().fromJson(json, type)
                medicines.find { it.id == medicineId }?.let {
                    com.example.medicinereminder.util.AlarmScheduler(context).scheduleAlarms(it)
                }
            }
        }

        val dosage = intent.getStringExtra("DOSAGE") ?: ""
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", medicineName)
            putExtra("DOSAGE", dosage)
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Service start failed", e)
        } finally {
            pendingResult.finish()
        }
    }
}
