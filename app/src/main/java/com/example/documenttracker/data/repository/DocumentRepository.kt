package com.example.documenttracker.data.repository

import com.example.documenttracker.data.model.Document
import com.example.documenttracker.data.model.DocumentHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DocumentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val docsRef = db.collection("documents")
    private val auth = FirebaseAuth.getInstance()

    suspend fun addDocument(document: Document) {
        val docRef = docsRef.document()
        val newDoc = document.copy(documentId = docRef.id)
        docRef.set(newDoc).await()
    }

    suspend fun updateDocumentStatus(documentId: String, status: String) {
        val userId = auth.currentUser?.uid ?: return
        val historyEntry = DocumentHistory(status, System.currentTimeMillis(), userId)

        val docRef = docsRef.document(documentId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentDoc = snapshot.toObject(Document::class.java)
            val newHistory = currentDoc?.history ?: mutableListOf()
            newHistory.add(historyEntry)
            transaction.update(docRef, mapOf(
                "status" to status,
                "history" to newHistory
            ))
        }.await()
    }

    suspend fun deleteDocument(documentId: String) {
        docsRef.document(documentId).delete().await()
    }

    suspend fun getUserDocuments(userId: String): List<Document> {
        return docsRef.whereEqualTo("ownerId", userId).get().await()
            .toObjects(Document::class.java)
    }

    suspend fun getAllDocuments(): List<Document> {
        return docsRef.get().await().toObjects(Document::class.java)
    }
}
