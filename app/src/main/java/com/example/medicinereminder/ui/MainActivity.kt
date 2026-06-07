package com.example.medicinereminder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.example.medicinereminder.adapter.MedicineAdapter
import com.example.medicinereminder.model.Medicine
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmpty: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var tvTodayCount: TextView
    private lateinit var tvTakenCount: TextView
    private var ivToolbarProfile: ShapeableImageView? = null

    private val allMedicines = mutableListOf<Medicine>()
    private val displayedMedicines = mutableListOf<Medicine>()

    companion object {
        const val REQUEST_ADD = 1001
        const val REQUEST_EDIT = 1002
        const val EXTRA_MEDICINE = "medicine"
        const val EXTRA_POSITION = "position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Firebase Authentication
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        tvEmpty = findViewById(R.id.tvEmpty)
        tabLayout = findViewById(R.id.tabLayout)
        tvTodayCount = findViewById(R.id.tvTodayCount)
        tvTakenCount = findViewById(R.id.tvTakenCount)

        setupRecyclerView()
        loadData()
        setupTabs()
        updateStats()

        com.example.medicinereminder.util.AlarmScheduler(this).rescheduleAllAlarms()

        checkPermissions()

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditMedicineActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD)
        }
    }

    override fun onResume() {
        super.onResume()
        updateToolbarProfileImage()
        loadData() // Refresh data and keep current tab
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(
            displayedMedicines,
            onEdit = { medicine ->
                val intent = Intent(this, AddEditMedicineActivity::class.java)
                intent.putExtra(EXTRA_MEDICINE, medicine)
                intent.putExtra(EXTRA_POSITION, allMedicines.indexOf(medicine))
                startActivityForResult(intent, REQUEST_EDIT)
            },
            onDelete = { medicine ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Medicine")
                    .setMessage("Are you sure you want to delete ${medicine.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        com.example.medicinereminder.util.AlarmScheduler(this).cancelAlarms(medicine)
                        allMedicines.remove(medicine)
                        saveData()
                        filterByTab(tabLayout.selectedTabPosition)
                        updateStats()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onToggleTaken = { medicine ->
                val index = allMedicines.indexOf(medicine)
                if (index >= 0) {
                    val isNowTaken = !medicine.isTaken
                    // Auto-decrease stock when marked as taken
                    var newStock = medicine.stockQuantity
                    if (isNowTaken && newStock > 0) {
                        newStock--
                    } else if (!isNowTaken) {
                        // Optional: increment back if untaken? 
                        // Let's keep it simple for now or maybe just leave it as is.
                    }
                    
                    allMedicines[index] = medicine.copy(isTaken = isNowTaken, stockQuantity = newStock)
                    saveData()
                    filterByTab(tabLayout.selectedTabPosition)
                    updateStats()
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadData() {
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Medicine>>() {}.type
            val savedMedicines: List<Medicine> = Gson().fromJson(json, type)
            allMedicines.clear()
            allMedicines.addAll(savedMedicines)
            checkDailyReset() // Check if we need to reset for a new day
        }
        
        // Refresh from Firebase
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            database.child(user.uid).child("medicines").get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val typeIndicator = object : com.google.firebase.database.GenericTypeIndicator<List<Medicine>>() {}
                    val firebaseMeds = snapshot.getValue(typeIndicator)
                    if (firebaseMeds != null) {
                        // Merge ora update only if local list is empty or stale
                        // For now, let's just ensure we don't overwrite local 'isTaken' if it's already true
                        if (allMedicines.isEmpty()) {
                            allMedicines.addAll(firebaseMeds)
                        } else {
                            // Update our local list with any new medicines from Firebase 
                            // but keep our local 'isTaken' status which is more up-to-date
                            firebaseMeds.forEach { fMed ->
                                val localIndex = allMedicines.indexOfFirst { it.id == fMed.id }
                                if (localIndex == -1) {
                                    allMedicines.add(fMed)
                                } else {
                                    // Update details but keep 'isTaken' if local is already true
                                    val currentLocal = allMedicines[localIndex]
                                    allMedicines[localIndex] = fMed.copy(
                                        isTaken = currentLocal.isTaken || fMed.isTaken,
                                        stockQuantity = Math.min(currentLocal.stockQuantity, fMed.stockQuantity)
                                    )
                                }
                            }
                        }
                        
                        checkDailyReset()
                        filterByTab(tabLayout.selectedTabPosition)
                        updateStats()
                        
                        val updatedJson = Gson().toJson(allMedicines)
                        prefs.edit().putString("medicine_list", updatedJson).apply()
                    }
                } else if (allMedicines.isEmpty()) {
                    loadSampleData()
                }
            }
        }
else if (allMedicines.isEmpty()) {
            loadSampleData()
        }
        filterByTab(tabLayout.selectedTabPosition) // Use current selected tab instead of 0
    }

    private fun checkDailyReset() {
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", "")
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        if (lastResetDate != today && allMedicines.isNotEmpty()) {
            // New day! Reset 'isTaken' for all medicines
            for (i in allMedicines.indices) {
                allMedicines[i] = allMedicines[i].copy(isTaken = false)
            }
            saveData()
            prefs.edit().putString("last_reset_date", today).apply()
        }
    }

    private fun saveData() {
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = Gson().toJson(allMedicines)
        prefs.edit().putString("medicine_list", json).apply()
        
        // Sync with Firebase
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            database.child(user.uid).child("medicines").setValue(allMedicines)
        }
    }

    private fun loadSampleData() {
        allMedicines.addAll(
            listOf(
                Medicine(1, "Metformin", "500mg", "Twice a day", listOf("08:00 AM", "08:00 PM"),
                    "01 Jan 2025", "31 Dec 2025", "Take with food", 0),
                Medicine(2, "Lisinopril", "10mg", "Once daily", listOf("09:00 AM"),
                    "15 Jan 2025", "15 Jul 2025", "Do not skip", 1),
                Medicine(3, "Vitamin D3", "1000 IU", "Once daily", listOf("07:00 AM"),
                    "01 Feb 2025", "01 Aug 2025", "Take with calcium", 2)
            )
        )
        saveData()
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Today"))
        tabLayout.addTab(tabLayout.newTab().setText("Taken"))
        tabLayout.addTab(tabLayout.newTab().setText("Pending"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { filterByTab(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun filterByTab(position: Int) {
        val filtered = when (position) {
            1 -> allMedicines
            2 -> allMedicines.filter { it.isTaken }
            3 -> allMedicines.filter { !it.isTaken }
            else -> allMedicines
        }
        displayedMedicines.clear()
        displayedMedicines.addAll(filtered)
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateStats() {
        tvTodayCount.text = allMedicines.size.toString()
        tvTakenCount.text = allMedicines.count { it.isTaken }.toString()
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        // Request Overlay Permission with Instruction Dialog
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog()
            }
            
            // Request Ignore Battery Optimization
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("To ensure alarms work even when the app is closed, please disable battery optimization for MedRemind.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("For alarms to appear on your lock screen, please allow 'Display over other apps'.\n\n" +
                    "NOTE: If the setting is blocked (Greyed out):\n" +
                    "1. Go to App Info for this app\n" +
                    "2. Tap the 3 dots (top right)\n" +
                    "3. Click 'Allow restricted settings'\n" +
                    "4. Then try this again.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText?.lowercase().orEmpty()
                val filtered = allMedicines.filter { it.name.lowercase().contains(q) }
                displayedMedicines.clear()
                displayedMedicines.addAll(filtered)
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                return true
            }
        })

        val profileItem = menu.findItem(R.id.action_profile)
        val actionView = profileItem.actionView
        ivToolbarProfile = actionView?.findViewById(R.id.ivToolbarProfile)
        
        actionView?.setOnClickListener {
            onOptionsItemSelected(profileItem)
        }
        
        updateToolbarProfileImage()
        
        return true
    }

    private fun updateToolbarProfileImage() {
        val imageView = ivToolbarProfile ?: return
        val prefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val imageUriString = prefs.getString("image_uri", null)
        
        if (imageUriString != null) {
            val uri = Uri.parse(imageUriString)
            try {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {}
                
                imageView.setImageURI(null)
                imageView.setImageURI(uri)
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.imageTintList = null
            } catch (e: Exception) {
                setDefaultProfileIcon(imageView)
            }
        } else {
            setDefaultProfileIcon(imageView)
        }
    }

    private fun setDefaultProfileIcon(imageView: ShapeableImageView) {
        imageView.setImageResource(R.drawable.ic_profile)
        imageView.setPadding(4, 4, 4, 4)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.imageTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(this, R.color.white)
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stock -> {
                startActivity(Intent(this, StockActivity::class.java))
                true
            }
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java).apply {
                    putExtra("total", allMedicines.size)
                    putExtra("taken", allMedicines.count { it.isTaken })
                })
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val medicine = data.getSerializableExtra(EXTRA_MEDICINE) as? Medicine ?: return
            val scheduler = com.example.medicinereminder.util.AlarmScheduler(this)
            
            when (requestCode) {
                REQUEST_ADD -> {
                    val newMed = medicine.copy(id = (allMedicines.maxOfOrNull { it.id } ?: 0) + 1)
                    allMedicines.add(newMed)
                    saveData()
                    scheduler.scheduleAlarms(newMed)
                }
                REQUEST_EDIT -> {
                    val position = data.getIntExtra(EXTRA_POSITION, -1)
                    if (position >= 0) {
                        scheduler.cancelAlarms(allMedicines[position])
                        allMedicines[position] = medicine
                        saveData()
                        scheduler.scheduleAlarms(medicine)
                    }
                }
            }
            filterByTab(tabLayout.selectedTabPosition)
            updateStats()
        }
    }
}
