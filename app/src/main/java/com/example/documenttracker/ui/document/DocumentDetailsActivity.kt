package com.example.documenttracker.ui.document

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.model.DocumentHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class DocumentDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var txtDetails: TextView
    private lateinit var spnStatusOptions: Spinner
    private lateinit var btnChangeStatus: Button
    private lateinit var btnBack: ImageView

    private var currentDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_details)

        txtDetails = findViewById(R.id.txtDocumentDetails)
        spnStatusOptions = findViewById(R.id.spnStatusOptions)
        btnChangeStatus = findViewById(R.id.btnChangeStatus)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        val documentId = intent.getStringExtra("documentId")
        val trackingNumber = intent.getStringExtra("trackingNumber")

        if (documentId != null) {
            currentDocId = documentId
            loadDocumentById(documentId)
        } else if (trackingNumber != null) {
            loadDocumentByTrackingNumber(trackingNumber)
        }

        btnChangeStatus.setOnClickListener {
            val newStatus = spnStatusOptions.selectedItem.toString()
            if (currentDocId != null) {
                updateDocumentStatus(currentDocId!!, newStatus)
            } else {
                Toast.makeText(this, "No document loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDocumentById(documentId: String) {
        db.collection("documents").document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val doc = snapshot.toObject(Document::class.java)
                displayDocument(doc)
            }
    }

    private fun loadDocumentByTrackingNumber(trackingNumber: String) {
        db.collection("documents").whereEqualTo("trackingNumber", trackingNumber)
            .addSnapshotListener { query, error ->
                if (error != null || query == null || query.isEmpty) return@addSnapshotListener
                val snapshot = query.documents.first()
                currentDocId = snapshot.id
                val doc = snapshot.toObject(Document::class.java)
                displayDocument(doc)
            }
    }

    private fun displayDocument(doc: Document?) {
        if (doc == null) return

        val historyText = if (doc.history.isNotEmpty()) {
            doc.history.joinToString("\n") {
                "- ${it.status} by ${it.updatedBy} on ${Date(it.timestamp)}"
            }
        } else "No history yet"

        txtDetails.text = """
            Tracking #: ${doc.trackingNumber}
            Description: ${doc.description}
            Status: ${doc.status}
            Sender: ${doc.senderId}
            Recipient: ${doc.recipientId}

            History:
            $historyText
        """.trimIndent()
    }

    private fun updateDocumentStatus(documentId: String, newStatus: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val docRef = db.collection("documents").document(documentId)
                val snapshot = docRef.get().await()
                val document = snapshot.toObject(Document::class.java) ?: return@launch

                val newHistory = document.history.toMutableList()
                newHistory.add(DocumentHistory(newStatus, System.currentTimeMillis(), userId))

                docRef.update(
                    mapOf(
                        "status" to newStatus,
                        "history" to newHistory
                    )
                ).await()

                runOnUiThread {
                    Toast.makeText(
                        this@DocumentDetailsActivity,
                        "Status updated to $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@DocumentDetailsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
