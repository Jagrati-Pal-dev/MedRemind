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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in via Firebase
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoToSignUp = findViewById<TextView>(R.id.tvGoToSignUp)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                btnLogin.isEnabled = false

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true

                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid
                            
                            if (userId != null) {
                                // Fetch user profile from Firebase
                                val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                                database.child(userId).get().addOnSuccessListener { snapshot ->
                                    if (snapshot.exists()) {
                                        val name = snapshot.child("name").value.toString()
                                        val email = snapshot.child("email").value.toString()
                                        
                                        // Save to local profile prefs
                                        val profilePrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                                        profilePrefs.edit().putString("name", name).putString("email", email).apply()
                                    }
                                }
                            }

                            // Legacy SharedPrefs support
                            val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("is_logged_in", true).apply()

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
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

        tvGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
