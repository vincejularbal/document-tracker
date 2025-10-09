package com.example.documenttracker.ui.document

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.model.User
import com.example.documenttracker.data.repository.DocumentRepository
import com.example.documenttracker.utils.QrUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddDocumentActivity : AppCompatActivity() {

    private val repo = DocumentRepository()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var edtDescription: EditText
    private lateinit var spnRecipients: Spinner
    private lateinit var btnSave: Button
    private lateinit var imgQrCode: ImageView
    private lateinit var btnBack: ImageView

    private val userList = mutableListOf<User>()
    private val userNames = mutableListOf<String>()
    private var selectedRecipientId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_document)

        // Initialize UI components
        edtDescription = findViewById(R.id.edtDescription)
        spnRecipients = findViewById(R.id.spnRecipients)
        btnSave = findViewById(R.id.btnSave)
        imgQrCode = findViewById(R.id.imgQrCode)
        btnBack = findViewById(R.id.btnBack)

        // Back navigation
        btnBack.setOnClickListener {
            finish()
        }

        // Load recipients list from Firestore
        loadRecipients()

        // Handle Save button click
        btnSave.setOnClickListener {
            val description = edtDescription.text.toString().trim()
            val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val recipientId = selectedRecipientId

            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (recipientId == null) {
                Toast.makeText(this, "Please select a recipient", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate unique tracking number
            val trackingNum = System.currentTimeMillis().toString()

            // Create document object
            val doc = Document(
                trackingNumber = trackingNum,
                senderId = senderId,
                recipientId = recipientId,
                description = description,
                ownerId = senderId
            )

            // Save document to Firestore and generate QR code
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repo.addDocument(doc)
                    val qr: Bitmap = QrUtils.generateQrCode(trackingNum)

                    runOnUiThread {
                        imgQrCode.setImageBitmap(qr)
                        Toast.makeText(
                            this@AddDocumentActivity,
                            "Document Added Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddDocumentActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun loadRecipients() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                userList.clear()
                userNames.clear()
                for (doc in result.documents) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        userList.add(user)
                        userNames.add(user.name)
                    }
                }

                if (userList.isEmpty()) {
                    Toast.makeText(this, "No recipients found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnRecipients.adapter = adapter

                spnRecipients.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedRecipientId = result.documents[position].id
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedRecipientId = null
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load recipients", Toast.LENGTH_SHORT).show()
            }
    }
}
