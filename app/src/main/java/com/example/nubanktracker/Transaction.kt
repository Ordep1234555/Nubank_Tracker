package com.example.nubanktracker

data class Transaction(
    val type: String,
    val amount: Double,
    val description: String,
    val date: String
)