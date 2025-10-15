package com.example.documenttracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.ui.auth.LoginActivity
import com.example.documenttracker.ui.document.DocumentDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmpty: TextView

    private val documentList = mutableListOf<Document>()
    private val displayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        // Initialize UI
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        listView = findViewById(R.id.listViewDocuments)
        progressBar = findViewById(R.id.progressBar)
        txtEmpty = findViewById(R.id.txtEmpty)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadUserDocuments()

        listView.setOnItemClickListener { _, _, position, _ ->
            val doc = documentList[position]
            val intent = Intent(this, DocumentDetailsActivity::class.java)
            intent.putExtra("documentId", doc.documentId)
            startActivity(intent)
        }
    }

    private fun loadUserDocuments() {
        val userId = auth.currentUser?.uid ?: return
        progressBar.visibility = android.view.View.VISIBLE
        txtEmpty.visibility = android.view.View.GONE

        // Documents where user is sender
        db.collection("documents")
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshots, error ->
                progressBar.visibility = android.view.View.GONE
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                documentList.clear()
                displayList.clear()

                if (snapshots == null || snapshots.isEmpty) {
                    txtEmpty.visibility = android.view.View.VISIBLE
                    return@addSnapshotListener
                }

                for (docSnap in snapshots.documents) {
                    val doc = docSnap.toObject(Document::class.java)
                    if (doc != null) {
                        val docWithId = doc.copy(documentId = docSnap.id)
                        documentList.add(docWithId)
                        displayList.add(
                            "Tracking #: ${docWithId.trackingNumber}\n" +
                                    "Description: ${docWithId.description}\n" +
                                    "Status: ${docWithId.status}"
                        )
                    }
                }

                adapter.notifyDataSetChanged()
            }

        // Also documents where user is recipient
        db.collection("documents")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                for (docSnap in snapshots.documents) {
                    val doc = docSnap.toObject(Document::class.java)
                    if (doc != null && documentList.none { it.documentId == docSnap.id }) {
                        val docWithId = doc.copy(documentId = docSnap.id)
                        documentList.add(docWithId)
                        displayList.add(
                            "Tracking #: ${docWithId.trackingNumber}\n" +
                                    "Description: ${docWithId.description}\n" +
                                    "Status: ${docWithId.status}"
                        )
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }
}
