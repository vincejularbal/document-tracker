package com.example.documenttracker.ui.document

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DocumentListActivity : AppCompatActivity() {

    private val repo = DocumentRepository()
    private lateinit var listView: ListView
    private var documents = listOf<Document>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_list)

        listView = findViewById(R.id.listDocuments)
        val isAdmin = intent.getBooleanExtra("isAdmin", false)

        CoroutineScope(Dispatchers.IO).launch {
            documents = if (isAdmin) {
                repo.getAllDocuments()
            } else {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                repo.getUserDocuments(userId)
            }

            runOnUiThread {
                val adapter = ArrayAdapter(
                    this@DocumentListActivity,
                    android.R.layout.simple_list_item_1,
                    documents.map { "${it.trackingNumber} - ${it.description}" }
                )
                listView.adapter = adapter
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDoc = documents[position]
            val intent = Intent(this, DocumentDetailsActivity::class.java)
            intent.putExtra("documentId", selectedDoc.documentId)
            startActivity(intent)
        }
    }
}
