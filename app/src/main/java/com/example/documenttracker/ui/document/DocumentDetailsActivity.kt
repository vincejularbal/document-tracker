package com.example.documenttracker.ui.document

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

class DocumentDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_details)

        val txtDetails = findViewById<TextView>(R.id.txtDocumentDetails)

        val documentId = intent.getStringExtra("documentId")
        val trackingNumber = intent.getStringExtra("trackingNumber")

        // Fetch document either by ID or trackingNumber
        if (documentId != null) {
            db.collection("documents").document(documentId).get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.toObject(Document::class.java)
                    if (doc != null) loadDetails(doc, txtDetails)
                }
        } else if (trackingNumber != null) {
            db.collection("documents").whereEqualTo("trackingNumber", trackingNumber)
                .get()
                .addOnSuccessListener { query ->
                    val snapshot = query.documents.firstOrNull()
                    val doc = snapshot?.toObject(Document::class.java)
                    if (doc != null) loadDetails(doc, txtDetails)
                }
        }
    }

    private fun loadDetails(document: Document, txtDetails: TextView) {
        // Fetch sender & recipient names
        db.collection("users").document(document.senderId).get()
            .addOnSuccessListener { senderSnap ->
                val sender = senderSnap.toObject(User::class.java)?.name ?: "Unknown Sender"

                db.collection("users").document(document.recipientId).get()
                    .addOnSuccessListener { recipientSnap ->
                        val recipient = recipientSnap.toObject(User::class.java)?.name ?: "Unknown Recipient"

                        // Now resolve history "updatedBy" names
                        val historyText = StringBuilder()
                        val history = document.history

                        if (history.isNotEmpty()) {
                            val tasks = history.map { h ->
                                db.collection("users").document(h.updatedBy).get()
                                    .continueWith { task ->
                                        val updater = task.result?.toObject(User::class.java)?.name ?: "Unknown"
                                        "${h.status} by $updater on ${java.util.Date(h.timestamp)}"
                                    }
                            }

                            // Wait for all tasks to finish
                            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                                .addOnSuccessListener { results ->
                                    val resolvedHistory = results.mapNotNull { it.result as? String }
                                    val details = """
                                        Tracking #: ${document.trackingNumber}
                                        Description: ${document.description}
                                        Status: ${document.status}
                                        Sender: $sender
                                        Recipient: $recipient
                                        
                                        History:
                                        ${resolvedHistory.joinToString("\n")}
                                    """.trimIndent()

                                    txtDetails.text = details
                                }
                        } else {
                            val details = """
                                Tracking #: ${document.trackingNumber}
                                Description: ${document.description}
                                Status: ${document.status}
                                Sender: $sender
                                Recipient: $recipient
                                History: No updates yet
                            """.trimIndent()

                            txtDetails.text = details
                        }
                    }
            }
    }
}
