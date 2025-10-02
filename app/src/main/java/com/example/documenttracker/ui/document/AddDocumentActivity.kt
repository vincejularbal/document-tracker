package com.example.documenttracker.ui.document

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.repository.DocumentRepository
import com.example.documenttracker.utils.QrUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddDocumentActivity : AppCompatActivity() {

    private val repo = DocumentRepository()
    private lateinit var qrImageView: ImageView
    private lateinit var edtDescription: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_document)

        // Initialize views
        edtDescription = findViewById(R.id.edtDescription)
        btnSave = findViewById(R.id.btnSave)
        qrImageView = findViewById(R.id.imgQrCode)

        btnSave.setOnClickListener {
            val description = edtDescription.text.toString().trim()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate a unique tracking number (timestamp-based for now)
            val trackingNum = System.currentTimeMillis().toString()

            // Build document object
            val doc = Document(
                trackingNumber = trackingNum,
                senderId = userId,
                recipientId = "", // will add user selection later
                description = description,
                ownerId = userId
            )

            // Save to Firestore using Coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.addDocument(doc)

                    // Generate QR Code bitmap
                    val qr: Bitmap = QrUtils.generateQrCode(trackingNum)

                    runOnUiThread {
                        qrImageView.setImageBitmap(qr)
                        Toast.makeText(this@AddDocumentActivity, "Document Added!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AddDocumentActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
