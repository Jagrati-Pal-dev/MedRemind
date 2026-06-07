package com.example.medicinereminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicinereminder.model.Medicine
import com.example.medicinereminder.util.AlarmScheduler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAlarms(context)
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val prefs = context.getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Medicine>>() {}.type
            val savedMedicines: List<Medicine> = Gson().fromJson(json, type)
            
            val scheduler = AlarmScheduler(context)
            savedMedicines.forEach { medicine ->
                scheduler.scheduleAlarms(medicine)
            }
        }
    }
}
