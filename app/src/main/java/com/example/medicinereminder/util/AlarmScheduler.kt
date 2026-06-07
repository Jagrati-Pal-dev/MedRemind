package com.example.medicinereminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.medicinereminder.model.Medicine
import com.example.medicinereminder.receiver.AlarmReceiver
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(medicine: Medicine) {
        Log.d("AlarmScheduler", "Scheduling alarms for ${medicine.name}")
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        // Schedule daily reminders
        medicine.times.forEachIndexed { index, timeString ->
            val calendar = Calendar.getInstance()
            val time = sdf.parse(timeString) ?: return@forEachIndexed
            
            val timeCalendar = Calendar.getInstance().apply {
                this.time = time
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)
            
            // If time is in the past, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                // High priority flags to wake up the app even if killed/stopped
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra("MEDICINE_ID", medicine.id)
                putExtra("MEDICINE_NAME", medicine.name)
                putExtra("DOSAGE", medicine.dosage)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.id * 100 + index,
                intent,
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            
            Log.d("AlarmScheduler", "Setting alarm for ${medicine.name} at ${calendar.time}")
            
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                calendar.timeInMillis,
                pendingIntent
            )
            
            alarmManager.setAlarmClock(
                alarmClockInfo,
                pendingIntent
            )
        }

        // Schedule Expiry Alert (2 days before)
        scheduleExpiryAlert(medicine)
    }

    fun rescheduleAllAlarms() {
        val prefs = context.getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Medicine>>() {}.type
            val savedMedicines: List<Medicine> = Gson().fromJson(json, type)
            savedMedicines.forEach { scheduleAlarms(it) }
            Log.d("AlarmScheduler", "All alarms rescheduled from list")
        }
    }

    private fun scheduleExpiryAlert(medicine: Medicine) {
        if (medicine.expiryDate.isEmpty()) return

        try {
            val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val expDate = sdf.parse(medicine.expiryDate) ?: return
            
            val calendar = Calendar.getInstance()
            calendar.time = expDate
            calendar.add(Calendar.DAY_OF_YEAR, -2) // 2 days before
            calendar.set(Calendar.HOUR_OF_DAY, 9) // Alert at 9:00 AM
            calendar.set(Calendar.MINUTE, 0)
            
            // Only schedule if it's in the future
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("MEDICINE_ID", medicine.id)
                    putExtra("MEDICINE_NAME", medicine.name)
                    putExtra("IS_EXPIRY_ALERT", true)
                    putExtra("EXPIRY_DATE", medicine.expiryDate)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicine.id * 100 + 88, // Unique ID for expiry
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Expiry alert scheduled for ${medicine.name} on ${calendar.time}")
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling expiry alert", e)
        }
    }

    fun snoozeAlarm(medicineId: Int, name: String, dosage: String) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 10)
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", name)
            putExtra("DOSAGE", dosage)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId * 100 + 99, // Unique snooze ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        Log.d("AlarmScheduler", "Snoozed $name for 10 mins at ${calendar.time}")
    }

    fun cancelAlarms(medicine: Medicine) {
        medicine.times.forEachIndexed { index, _ ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.id * 100 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        
        // Cancel Expiry Alert
        val expIntent = Intent(context, AlarmReceiver::class.java)
        val expPendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id * 100 + 88,
            expIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(expPendingIntent)
    }
}
