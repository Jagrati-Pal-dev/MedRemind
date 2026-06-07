package com.example.medicinereminder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.R
import com.example.medicinereminder.model.Medicine

class MedicineAdapter(
    private val medicines: MutableList<Medicine>,
    private val onEdit: (Medicine) -> Unit,
    private val onDelete: (Medicine) -> Unit,
    private val onToggleTaken: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    private val cardColors = intArrayOf(
        R.color.card_blue,
        R.color.card_green,
        R.color.card_orange,
        R.color.card_purple,
        R.color.card_pink
    )

    inner class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val tvName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvDosage: TextView = itemView.findViewById(R.id.tvDosage)
        val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        val tvTimes: TextView = itemView.findViewById(R.id.tvTimes)
        val tvDates: TextView = itemView.findViewById(R.id.tvDates)
        val tvStock: TextView = itemView.findViewById(R.id.tvStockBadge)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnTaken: ImageButton = itemView.findViewById(R.id.btnTaken)
        val tvTakenBadge: TextView = itemView.findViewById(R.id.tvTakenBadge)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivMedicineThumb)
        val ivInstruction: ImageView = itemView.findViewById(R.id.ivItemInstructionIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicines[position]
        holder.tvName.text = medicine.name
        holder.tvDosage.text = medicine.dosage
        holder.tvFrequency.text = medicine.frequency
        holder.tvTimes.text = "⏰ " + medicine.times.joinToString(", ")
        holder.tvDates.text = "📅 ${medicine.startDate}  →  ${medicine.endDate}"
        holder.tvStock.text = holder.itemView.context.getString(R.string.label_stock_count, medicine.stockQuantity)
        holder.tvStock.visibility = if (medicine.stockQuantity > 0) View.VISIBLE else View.GONE

        val colorRes = cardColors[medicine.color % cardColors.size]
        holder.cardView.setCardBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, colorRes)
        )

        holder.tvTakenBadge.visibility = if (medicine.isTaken) View.VISIBLE else View.GONE
        holder.btnTaken.setImageResource(
            if (medicine.isTaken) R.drawable.ic_check_circle else R.drawable.ic_check_outline
        )

        if (medicine.imageUri != null) {
            holder.ivThumb.setImageURI(android.net.Uri.parse(medicine.imageUri))
            holder.ivThumb.alpha = 1.0f
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_check_circle)
            holder.ivThumb.alpha = 0.5f
        }

        when (medicine.instructions) {
            "Empty Stomach" -> {
                holder.ivInstruction.setImageResource(R.drawable.ic_empty_stomach)
                holder.ivInstruction.visibility = View.VISIBLE
            }
            "After Food" -> {
                holder.ivInstruction.setImageResource(R.drawable.ic_after_food)
                holder.ivInstruction.visibility = View.VISIBLE
            }
            "With Milk" -> {
                holder.ivInstruction.setImageResource(R.drawable.ic_with_milk)
                holder.ivInstruction.visibility = View.VISIBLE
            }
            "Before Sleep" -> {
                holder.ivInstruction.setImageResource(R.drawable.ic_before_sleep)
                holder.ivInstruction.visibility = View.VISIBLE
            }
            else -> holder.ivInstruction.visibility = View.GONE
        }

        holder.btnEdit.setOnClickListener { onEdit(medicine) }
        holder.btnDelete.setOnClickListener { onDelete(medicine) }
        holder.btnTaken.setOnClickListener { onToggleTaken(medicine) }
    }

    override fun getItemCount() = medicines.size

    fun updateList(newList: List<Medicine>) {
        medicines.clear()
        medicines.addAll(newList)
        notifyDataSetChanged()
    }
}
