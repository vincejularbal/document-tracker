package com.example.documenttracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.ui.document.DocumentListActivity
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val btnAllDocs = findViewById<Button>(R.id.btnAllDocuments)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnAllDocs.setOnClickListener {
            // Admin can view ALL documents
            val intent = Intent(this, DocumentListActivity::class.java)
            intent.putExtra("isAdmin", true)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }
}
