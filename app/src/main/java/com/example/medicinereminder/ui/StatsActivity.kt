package com.example.medicinereminder.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.medicinereminder.R

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        val toolbar = findViewById<Toolbar>(R.id.toolbarStats)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Progress"

        val total = intent.getIntExtra("total", 0)
        val taken = intent.getIntExtra("taken", 0)
        val pending = total - taken
        val percentage = if (total > 0) (taken * 100) / total else 0

        findViewById<TextView>(R.id.tvTotalMeds).text = total.toString()
        findViewById<TextView>(R.id.tvTakenMeds).text = taken.toString()
        findViewById<TextView>(R.id.tvPendingMeds).text = pending.toString()
        findViewById<TextView>(R.id.tvAdherencePercent).text = "$percentage%"
        findViewById<ProgressBar>(R.id.progressAdherence).progress = percentage

        val message = when {
            percentage == 100 -> "🏆 Perfect! Keep it up!"
            percentage >= 75 -> "😊 Great job! Almost there."
            percentage >= 50 -> "👍 Good, but try to do better!"
            else -> "⚠️ Please take your medicines on time."
        }
        findViewById<TextView>(R.id.tvMotivation).text = message

        // Mock weekly data
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayTaken = listOf(3, 3, 2, 3, 1, 3, taken)
        val dayTotal = listOf(3, 3, 3, 3, 3, 3, total)

        val weekRows = listOf(
            R.id.row0, R.id.row1, R.id.row2, R.id.row3,
            R.id.row4, R.id.row5, R.id.row6
        )
        weekRows.forEachIndexed { i, rowId ->
            val row = findViewById<android.widget.LinearLayout>(rowId)
            row.findViewWithTag<TextView>("day_label")?.text = days[i]
            row.findViewWithTag<TextView>("day_taken")?.text = "${dayTaken[i]}/${dayTotal[i]}"
            row.findViewWithTag<ProgressBar>("day_progress")?.apply {
                max = dayTotal[i]
                progress = dayTaken[i]
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
