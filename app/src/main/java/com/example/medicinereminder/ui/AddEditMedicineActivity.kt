package com.example.medicinereminder.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.medicinereminder.R
import com.example.medicinereminder.model.Medicine
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddEditMedicineActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etDosage: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var spinnerFrequency: Spinner
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etStock: TextInputEditText
    private lateinit var etMfgDate: TextInputEditText
    private lateinit var etExpDate: TextInputEditText
    private lateinit var chipGroupTimes: ChipGroup
    private lateinit var btnAddTime: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var colorGroup: RadioGroup
    private lateinit var ivMedicineImage: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var chipGroupInstructions: ChipGroup

    private val selectedTimes = mutableListOf<String>()
    private var editMedicine: Medicine? = null
    private var editPosition: Int = -1
    private var selectedImageUri: String? = null
    private var currentPhotoPath: String? = null

    private val frequencies = arrayOf("Once daily", "Twice a day", "Three times a day", "Weekly", "As needed")

    companion object {
        private const val PICK_IMAGE_REQUEST = 2001
        private const val CAMERA_REQUEST = 2002
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_medicine)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAddEdit)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etName = findViewById(R.id.etMedicineName)
        etDosage = findViewById(R.id.etDosage)
        etNotes = findViewById(R.id.etNotes)
        spinnerFrequency = findViewById(R.id.spinnerFrequency)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        etStock = findViewById(R.id.etStock)
        etMfgDate = findViewById(R.id.etMfgDate)
        etExpDate = findViewById(R.id.etExpDate)
        chipGroupTimes = findViewById(R.id.chipGroupTimes)
        btnAddTime = findViewById(R.id.btnAddTime)
        btnSave = findViewById(R.id.btnSave)
        colorGroup = findViewById(R.id.colorGroup)
        ivMedicineImage = findViewById(R.id.ivMedicineImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        chipGroupInstructions = findViewById(R.id.chipGroupInstructions)

        // Custom adapter for visibility
        val adapter = ArrayAdapter(this, R.layout.spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrequency.adapter = adapter

        // Check if editing
        @Suppress("DEPRECATION")
        editMedicine = intent.getSerializableExtra(MainActivity.EXTRA_MEDICINE) as? Medicine
        editPosition = intent.getIntExtra(MainActivity.EXTRA_POSITION, -1)

        if (editMedicine != null) {
            supportActionBar?.title = "Edit Medicine"
            populateFields(editMedicine!!)
        } else {
            supportActionBar?.title = "Add Medicine"
            // Default dates
            val cal = Calendar.getInstance()
            etStartDate.setText("${cal.get(Calendar.DAY_OF_MONTH)} ${monthName(cal.get(Calendar.MONTH))} ${cal.get(Calendar.YEAR)}")
            cal.add(Calendar.MONTH, 3)
            etEndDate.setText("${cal.get(Calendar.DAY_OF_MONTH)} ${monthName(cal.get(Calendar.MONTH))} ${cal.get(Calendar.YEAR)}")
        }

        btnAddTime.setOnClickListener { showTimePicker() }
        btnSave.setOnClickListener { saveMedicine() }
        btnSelectImage.setOnClickListener { showImageSourceDialog() }

        // Date pickers
        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
        etMfgDate.setOnClickListener { showDatePicker(etMfgDate) }
        etExpDate.setOnClickListener { showDatePicker(etExpDate) }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.option_take_photo),
            getString(R.string.option_choose_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_photo_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            return
        }

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // On Android 11+ resolveActivity might fail due to package visibility
            // We've added <queries> to manifest, but let's be more robust
            try {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    Log.e("AddEditMedicine", "Error creating image file", ex)
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    // Grant permission to the camera app
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST)
                }
            } catch (e: Exception) {
                Log.e("AddEditMedicine", "Error starting camera", e)
                Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        selectedImageUri = uri.toString()
                        ivMedicineImage.setImageURI(uri)
                        ivMedicineImage.alpha = 1.0f
                    }
                }
                CAMERA_REQUEST -> {
                    currentPhotoPath?.let { path ->
                        val file = File(path)
                        val uri = Uri.fromFile(file)
                        selectedImageUri = uri.toString()
                        ivMedicineImage.setImageURI(uri)
                        ivMedicineImage.alpha = 1.0f
                    }
                }
            }
        }
    }

    private fun populateFields(med: Medicine) {
        etName.setText(med.name)
        etDosage.setText(med.dosage)
        etNotes.setText(med.notes)
        etStartDate.setText(med.startDate)
        etEndDate.setText(med.endDate)
        etStock.setText(med.stockQuantity.toString())
        etMfgDate.setText(med.manufacturingDate)
        etExpDate.setText(med.expiryDate)

        val freqIndex = frequencies.indexOf(med.frequency)
        if (freqIndex >= 0) spinnerFrequency.setSelection(freqIndex)

        med.times.forEach { addTimeChip(it) }

        val radioIds = intArrayOf(R.id.rbBlue, R.id.rbGreen, R.id.rbOrange, R.id.rbPurple, R.id.rbPink)
        if (med.color in radioIds.indices) colorGroup.check(radioIds[med.color])

        selectedImageUri = med.imageUri
        if (selectedImageUri != null) {
            ivMedicineImage.setImageURI(Uri.parse(selectedImageUri))
            ivMedicineImage.alpha = 1.0f
        }

        when (med.instructions) {
            "Empty Stomach" -> chipGroupInstructions.check(R.id.chipEmptyStomach)
            "After Food" -> chipGroupInstructions.check(R.id.chipAfterFood)
            "With Milk" -> chipGroupInstructions.check(R.id.chipWithMilk)
            "Before Sleep" -> chipGroupInstructions.check(R.id.chipBeforeSleep)
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val time = String.format("%02d:%02d %s", displayHour, minute, amPm)
            addTimeChip(time)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun addTimeChip(time: String) {
        if (selectedTimes.contains(time)) return
        selectedTimes.add(time)
        val chip = Chip(this)
        chip.text = time
        chip.isCloseIconVisible = true
        chip.setChipBackgroundColorResource(R.color.chip_background)
        chip.setOnCloseIconClickListener {
            selectedTimes.remove(time)
            chipGroupTimes.removeView(chip)
        }
        chipGroupTimes.addView(chip)
    }

    private fun saveMedicine() {
        val name = etName.text?.toString()?.trim() ?: ""
        val dosage = etDosage.text?.toString()?.trim() ?: ""
        val startDate = etStartDate.text?.toString()?.trim() ?: ""
        val endDate = etEndDate.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) { etName.error = "Required"; return }
        if (dosage.isEmpty()) { etDosage.error = "Required"; return }
        if (selectedTimes.isEmpty()) {
            Toast.makeText(this, "Please add at least one reminder time", Toast.LENGTH_SHORT).show()
            return
        }

        val colorIndex = when (colorGroup.checkedRadioButtonId) {
            R.id.rbGreen -> 1
            R.id.rbOrange -> 2
            R.id.rbPurple -> 3
            R.id.rbPink -> 4
            else -> 0
        }

        val selectedInstructionId = chipGroupInstructions.checkedChipId
        val instructions = when (selectedInstructionId) {
            R.id.chipEmptyStomach -> "Empty Stomach"
            R.id.chipAfterFood -> "After Food"
            R.id.chipWithMilk -> "With Milk"
            R.id.chipBeforeSleep -> "Before Sleep"
            else -> null
        }

        val medicine = Medicine(
            id = editMedicine?.id ?: 0,
            name = name,
            dosage = dosage,
            frequency = spinnerFrequency.selectedItem.toString(),
            times = selectedTimes.toList(),
            startDate = startDate,
            endDate = endDate,
            notes = etNotes.text?.toString()?.trim() ?: "",
            color = colorIndex,
            isTaken = editMedicine?.isTaken ?: false,
            imageUri = selectedImageUri,
            instructions = instructions,
            stockQuantity = etStock.text?.toString()?.toIntOrNull() ?: 0,
            manufacturingDate = etMfgDate.text?.toString() ?: "",
            expiryDate = etExpDate.text?.toString() ?: ""
        )

        val resultIntent = Intent()
        resultIntent.putExtra(MainActivity.EXTRA_MEDICINE, medicine)
        resultIntent.putExtra(MainActivity.EXTRA_POSITION, editPosition)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showDatePicker(target: TextInputEditText) {
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(this, { _, year, month, day ->
            target.setText("$day ${monthName(month)} $year")
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun monthName(month: Int) = arrayOf(
        "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    )[month]

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
