package com.example.documenttracker.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user" // "admin" or "user"
)