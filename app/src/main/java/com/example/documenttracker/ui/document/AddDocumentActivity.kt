package com.example.documenttracker.ui.document

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.*

class AddDocumentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var edtDescription: EditText
    private lateinit var spnRecipients: Spinner
    private lateinit var btnSave: Button
    private lateinit var imgQrCode: ImageView
    private lateinit var btnBack: ImageView

    private var recipientId: String? = null
    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_document)

        edtDescription = findViewById(R.id.edtDescription)
        spnRecipients = findViewById(R.id.spnRecipients)
        btnSave = findViewById(R.id.btnSave)
        imgQrCode = findViewById(R.id.imgQrCode)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        loadRecipients()

        btnSave.setOnClickListener {
            val description = edtDescription.text.toString().trim()
            val senderId = auth.currentUser?.uid ?: return@setOnClickListener
            val recipient = recipientId

            if (description.isEmpty() || recipient == null) {
                Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val trackingNumber = generateTrackingNumber()

            // ✅ Firestore data with server timestamp
            val docData = hashMapOf(
                "trackingNumber" to trackingNumber,
                "description" to description,
                "senderId" to senderId,
                "recipientId" to recipient,
                "status" to "Created",
                "ownerId" to senderId,
                "dateCreated" to FieldValue.serverTimestamp(), // ✅ Timestamp field
                "history" to emptyList<Map<String, Any>>()
            )

            db.collection("documents")
                .add(docData)
                .addOnSuccessListener { documentRef ->
                    Toast.makeText(this, "Document added successfully!", Toast.LENGTH_SHORT).show()
                    generateAndDisplayQrCode(trackingNumber)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadRecipients() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val userList = mutableListOf<String>()
                val userIds = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "(No Name)"
                    userList.add(name)
                    userIds.add(doc.id)
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnRecipients.adapter = adapter

                spnRecipients.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        recipientId = userIds[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        recipientId = null
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading recipients: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateTrackingNumber(): String {
        val random = (100000..999999).random()
        return "DOC-$random"
    }

    private fun generateAndDisplayQrCode(data: String) {
        try {
            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            qrBitmap = bmp
            imgQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
