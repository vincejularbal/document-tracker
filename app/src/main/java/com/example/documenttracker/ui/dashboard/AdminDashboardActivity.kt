package com.example.documenttracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.ui.admin.AdminDocumentsActivity
import com.example.documenttracker.ui.admin.ManageUsersActivity
import com.example.documenttracker.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var btnViewDocuments: Button
    private lateinit var btnManageUsers: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        btnLogout = findViewById(R.id.btnLogout)
        btnViewDocuments = findViewById(R.id.btnViewDocuments)
        btnManageUsers = findViewById(R.id.btnManageUsers)

        // View all documents created by all users
        btnViewDocuments.setOnClickListener {
            val intent = Intent(this, AdminDocumentsActivity::class.java)
            startActivity(intent)
        }

        // Manage Users (CRUD)
        btnManageUsers.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            startActivity(intent)
        }

        // Logout
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
