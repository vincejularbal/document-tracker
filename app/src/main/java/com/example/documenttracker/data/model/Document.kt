package com.example.documenttracker.data.model

data class Document(
    val documentId: String = "",       // Firestore doc id
    val trackingNumber: String = "",   // unique code (QR code value)
    val senderId: String = "",         // reference to User
    val recipientId: String = "",      // reference to User
    val description: String = "",
    val status: String = "Created",
    val ownerId: String = "",          // user who created it
    val dateCreated: Long = System.currentTimeMillis(),  // 🕒 timestamp
    val history: MutableList<DocumentHistory> = mutableListOf()
)
