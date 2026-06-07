package com.example.medicinereminder.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicinereminder.R
import com.example.medicinereminder.model.Medicine
import com.example.medicinereminder.util.AlarmScheduler
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Comprehensive Lock Screen Setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        // Ensure screen stays on while alarm is ringing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm)

        val medicineId = intent.getIntExtra("MEDICINE_ID", -1)
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val dosage = intent.getStringExtra("DOSAGE") ?: ""

        val medicine = findMedicineById(medicineId)

        findViewById<TextView>(R.id.tvMedicineName).text = medicineName
        findViewById<TextView>(R.id.tvDosage).text = dosage

        val ivPhoto = findViewById<ImageView>(R.id.ivMedicinePhoto)
        val ivInstruction = findViewById<ImageView>(R.id.ivInstructionIcon)

        if (medicine != null) {
            if (medicine.imageUri != null) {
                ivPhoto.setImageURI(android.net.Uri.parse(medicine.imageUri))
            }
            
            when (medicine.instructions) {
                "Empty Stomach" -> {
                    ivInstruction.setImageResource(R.drawable.ic_empty_stomach)
                    ivInstruction.visibility = View.VISIBLE
                }
                "After Food" -> {
                    ivInstruction.setImageResource(R.drawable.ic_after_food)
                    ivInstruction.visibility = View.VISIBLE
                }
                "With Milk" -> {
                    ivInstruction.setImageResource(R.drawable.ic_with_milk)
                    ivInstruction.visibility = View.VISIBLE
                }
                "Before Sleep" -> {
                    ivInstruction.setImageResource(R.drawable.ic_before_sleep)
                    ivInstruction.visibility = View.VISIBLE
                }
                else -> ivInstruction.visibility = View.GONE
            }
        }

        findViewById<MaterialButton>(R.id.btnTake).setOnClickListener {
            Log.d("AlarmActivity", "Take button clicked")
            markAsTaken(medicineId)
            stopAlarmService()
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSnooze).setOnClickListener {
            AlarmScheduler(this).snoozeAlarm(medicineId, medicineName, dosage)
            stopAlarmService()
            finish()
        }

        findViewById<MaterialButton>(R.id.btnDismiss).setOnClickListener {
            stopAlarmService()
            finish()
        }
    }

    private fun findMedicineById(id: Int): Medicine? {
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Medicine>>() {}.type
            val medicines: List<Medicine> = Gson().fromJson(json, type)
            return medicines.find { it.id == id }
        }
        return null
    }

    private fun markAsTaken(medicineId: Int) {
        if (medicineId == -1) return
        
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Medicine>>() {}.type
            val medicines: MutableList<Medicine> = Gson().fromJson(json, type)
            
            val index = medicines.indexOfFirst { it.id == medicineId }
            if (index != -1) {
                val med = medicines[index]
                val newStock = if (med.stockQuantity > 0) med.stockQuantity - 1 else med.stockQuantity
                medicines[index] = med.copy(isTaken = true, stockQuantity = newStock)
                
                val newJson = Gson().toJson(medicines)
                prefs.edit().putString("medicine_list", newJson).apply()
                
                // Sync with Firebase immediately
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(user.uid)
                        .child("medicines")
                        .setValue(medicines)
                }
                
                Log.d("AlarmActivity", "Medicine marked as taken and synced: $medicineId")
            }
        }
    }

    private fun stopAlarmService() {
        val intent = android.content.Intent(this, com.example.medicinereminder.service.AlarmService::class.java)
        stopService(intent)
    }

    override fun onBackPressed() {
        // Disable back button
    }
}
