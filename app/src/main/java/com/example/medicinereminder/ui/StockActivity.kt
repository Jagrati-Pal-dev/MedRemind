package com.example.medicinereminder.ui

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.example.medicinereminder.model.Medicine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class StockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock)

        val toolbar = findViewById<Toolbar>(R.id.toolbarStock)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val rvStock = findViewById<RecyclerView>(R.id.rvStock)
        rvStock.layoutManager = LinearLayoutManager(this)
        
        val medicines = loadMedicines()
        rvStock.adapter = StockAdapter(medicines)
    }

    private fun loadMedicines(): List<Medicine> {
        val prefs = getSharedPreferences("medicines", Context.MODE_PRIVATE)
        val json = prefs.getString("medicine_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Medicine>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class StockAdapter(private val list: List<Medicine>) : RecyclerView.Adapter<StockAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_stock, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val med = list[position]
            holder.name.text = med.name
            holder.quantity.text = getString(R.string.label_stock_count, med.stockQuantity)
            holder.mfgDate.text = med.manufacturingDate.ifEmpty { "N/A" }
            holder.expDate.text = med.expiryDate.ifEmpty { "N/A" }

            if (med.expiryDate.isNotEmpty()) {
                try {
                    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    val expDate = sdf.parse(med.expiryDate)
                    if (expDate != null) {
                        val diff = expDate.time - System.currentTimeMillis()
                        val daysLeft = diff / (1000 * 60 * 60 * 24)
                        if (daysLeft in 0..7) {
                            holder.warning.visibility = View.VISIBLE
                            holder.warning.text = getString(R.string.warning_expiring_soon, daysLeft.toInt())
                        } else if (daysLeft < 0) {
                            holder.warning.visibility = View.VISIBLE
                            holder.warning.text = getString(R.string.status_expired)
                            holder.warning.setTextColor(getColor(R.color.card_pink))
                        } else {
                            holder.warning.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    holder.warning.visibility = View.GONE
                }
            } else {
                holder.warning.visibility = View.GONE
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name = v.findViewById<TextView>(R.id.tvStockMedName)
            val quantity = v.findViewById<TextView>(R.id.tvStockQuantity)
            val mfgDate = v.findViewById<TextView>(R.id.tvStockMfgDate)
            val expDate = v.findViewById<TextView>(R.id.tvStockExpDate)
            val warning = v.findViewById<TextView>(R.id.tvExpWarning)
        }
    }
}
