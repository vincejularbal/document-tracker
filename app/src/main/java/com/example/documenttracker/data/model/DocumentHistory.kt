package com.example.documenttracker.data.model

data class DocumentHistory(
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedBy: String = "" // userId
)