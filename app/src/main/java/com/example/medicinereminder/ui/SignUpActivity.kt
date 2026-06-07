package com.example.medicinereminder.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicinereminder.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarSignUp)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                progressBar.visibility = View.VISIBLE
                btnSignUp.isEnabled = false

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true

                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid
                            
                            // Save additional user info in Realtime Database
                            if (userId != null) {
                                val database = FirebaseDatabase.getInstance().getReference("users")
                                val userInfo = mapOf("name" to name, "email" to email)
                                database.child(userId).setValue(userInfo)
                            }

                            // Legacy SharedPrefs support
                            val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("is_logged_in", true).apply()
                            
                            val profilePrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                            profilePrefs.edit().putString("name", name).putString("email", email).apply()

                            Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
