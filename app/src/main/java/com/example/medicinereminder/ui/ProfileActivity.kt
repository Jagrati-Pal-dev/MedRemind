package com.example.medicinereminder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.ImageViewCompat
import com.example.medicinereminder.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var fabEditPhoto: FloatingActionButton
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val getImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedImageUri = it
                updateProfileImageUI(it)
            } catch (e: Exception) {
                selectedImageUri = it
                updateProfileImageUI(it)
            }
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                val uri = Uri.fromFile(file)
                selectedImageUri = uri
                updateProfileImageUI(uri)
            }
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbarProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        fabEditPhoto = findViewById(R.id.fabEditPhoto)
        etName = findViewById(R.id.etProfileName)
        etEmail = findViewById(R.id.etProfileEmail)
        etPhone = findViewById(R.id.etProfilePhone)
        btnSave = findViewById(R.id.btnSaveProfile)
        btnLogout = findViewById(R.id.btnLogout)

        loadProfileData()

        fabEditPhoto.setOnClickListener {
            showImageSourceDialog()
        }

        btnSave.setOnClickListener {
            saveProfileData()
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val sessionPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            sessionPrefs.edit().putBoolean("is_logged_in", false).apply()
            
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera()
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> getImage.launch(arrayOf("image/*"))
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            Log.e("ProfileActivity", "Error creating image file", ex)
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            takePhoto.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PROFILE_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun updateProfileImageUI(uri: Uri?) {
        if (uri != null) {
            ivProfilePicture.setImageURI(null)
            ivProfilePicture.setImageURI(uri)
            ivProfilePicture.setPadding(0, 0, 0, 0)
            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
            ImageViewCompat.setImageTintList(ivProfilePicture, null)
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile)
            ivProfilePicture.setPadding(60, 60, 60, 60)
            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val primaryColor = ContextCompat.getColor(this, R.color.primary)
            ImageViewCompat.setImageTintList(ivProfilePicture, ColorStateList.valueOf(primaryColor))
        }
    }

    private fun loadProfileData() {
        val prefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("name", "John Doe"))
        etEmail.setText(prefs.getString("email", "john.doe@example.com"))
        etPhone.setText(prefs.getString("phone", "+1 234 567 890"))
        
        val imageUriString = prefs.getString("image_uri", null)
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            try {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}
                updateProfileImageUI(uri)
            } catch (e: Exception) {
                updateProfileImageUI(null)
            }
        } else {
            updateProfileImageUI(null)
        }
    }

    private fun saveProfileData() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("name", name)
            putString("email", email)
            putString("phone", phone)
            if (selectedImageUri != null) {
                putString("image_uri", selectedImageUri.toString())
            }
            apply()
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
