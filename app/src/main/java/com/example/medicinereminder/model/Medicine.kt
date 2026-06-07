package com.example.medicinereminder.model

import java.io.Serializable

data class Medicine(
    val id: Int = 0,
    val name: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val times: List<String> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
    val color: Int = 0,
    val isTaken: Boolean = false,
    val imageUri: String? = null,
    val instructions: String? = null,
    val stockQuantity: Int = 0,
    val manufacturingDate: String = "",
    val expiryDate: String = ""
) : Serializable
