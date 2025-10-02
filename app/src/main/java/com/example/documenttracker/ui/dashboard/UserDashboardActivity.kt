package com.example.documenttracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.ui.document.AddDocumentActivity
import com.example.documenttracker.ui.document.DocumentListActivity
import com.example.documenttracker.ui.document.DocumentDetailsActivity
import com.example.documenttracker.utils.QrScanContract
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.ScanOptions

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var qrLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        val btnAddDoc = findViewById<Button>(R.id.btnAddDocument)
        val btnMyDocs = findViewById<Button>(R.id.btnMyDocuments)
        val btnScanQr = findViewById<Button>(R.id.btnScanQr)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Register QR launcher
        qrLauncher = registerForActivityResult(QrScanContract()) { result ->
            if (result != null) {
                val intent = Intent(this, DocumentDetailsActivity::class.java)
                intent.putExtra("trackingNumber", result)
                startActivity(intent)
            }
        }

        btnAddDoc.setOnClickListener {
            startActivity(Intent(this, AddDocumentActivity::class.java))
        }
        btnMyDocs.setOnClickListener {
            startActivity(Intent(this, DocumentListActivity::class.java))
        }
        btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("Scan Document QR Code")
                setOrientationLocked(false)
            }
            qrLauncher.launch(options)
        }
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }
}
