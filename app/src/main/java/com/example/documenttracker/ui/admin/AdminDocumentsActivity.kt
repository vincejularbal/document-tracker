package com.example.documenttracker.ui.admin

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.example.documenttracker.data.model.Document
import com.example.documenttracker.ui.document.DocumentDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.text.SimpleDateFormat
import java.util.*

class AdminDocumentsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tableLayout: TableLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_documents)

        tableLayout = findViewById(R.id.tableLayoutDocuments)
        progressBar = findViewById(R.id.progressBar)
        txtEmpty = findViewById(R.id.txtEmpty)
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)

        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }

        listenToDocumentsRealtime()
    }

    /**
     * Real-time Firestore listener for documents collection
     */
    private fun listenToDocumentsRealtime() {
        progressBar.visibility = android.view.View.VISIBLE
        txtEmpty.visibility = android.view.View.GONE
        tableLayout.removeAllViews()
        addTableHeader()

        db.collection("documents")
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                progressBar.visibility = android.view.View.GONE

                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    txtEmpty.visibility = android.view.View.VISIBLE
                    tableLayout.removeAllViews()
                    addTableHeader()
                    return@addSnapshotListener
                }

                // ✅ Clear and rebuild table
                tableLayout.removeAllViews()
                addTableHeader()
                txtEmpty.visibility = android.view.View.GONE

                for (docSnap in snapshots.documents) {
                    val doc = docSnap.toObject(Document::class.java)
                    if (doc != null) {
                        val documentId = docSnap.id
                        val trackingNumber = doc.trackingNumber
                        val description = doc.description
                        val senderId = doc.senderId
                        val timestamp = (docSnap.getTimestamp("dateCreated")?.toDate()?.time)
                            ?: docSnap.getLong("dateCreated")
                            ?: 0L
                        val dateString = if (timestamp > 0)
                            dateFormatter.format(Date(timestamp))
                        else "N/A"

                        // Fetch sender name asynchronously
                        db.collection("users").document(senderId).get()
                            .addOnSuccessListener { senderDoc ->
                                val senderName = senderDoc.getString("name") ?: "Unknown"
                                val qrBitmap = generateQrCode(trackingNumber)
                                addTableRow(documentId, trackingNumber, description, senderName, dateString, qrBitmap)
                            }
                            .addOnFailureListener {
                                val qrBitmap = generateQrCode(trackingNumber)
                                addTableRow(documentId, trackingNumber, description, "Unknown", dateString, qrBitmap)
                            }
                    }
                }
            }
    }

    private fun addTableHeader() {
        val headerRow = TableRow(this)
        val headers = listOf("Tracking #", "Description", "Sender", "Date Sent", "QR Code")

        for (header in headers) {
            val textView = TextView(this)
            textView.text = header
            textView.textSize = 14f
            textView.setPadding(12, 12, 12, 12)
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
            textView.gravity = Gravity.CENTER
            headerRow.addView(textView)
        }

        tableLayout.addView(headerRow)
    }

    private fun addTableRow(
        documentId: String,
        trackingNumber: String,
        description: String,
        sender: String,
        dateSent: String,
        qrBitmap: Bitmap
    ) {
        val row = TableRow(this)
        row.setPadding(4, 8, 4, 8)
        row.isClickable = true
        row.isFocusable = true
        row.setBackgroundResource(android.R.drawable.list_selector_background)

        val trackingView = makeCell(trackingNumber)
        val descView = makeCell(description)
        val senderView = makeCell(sender)
        val dateView = makeCell(dateSent)

        val qrView = ImageView(this)
        qrView.setImageBitmap(qrBitmap)
        qrView.adjustViewBounds = true
        qrView.maxHeight = 100
        qrView.maxWidth = 100
        qrView.setPadding(8, 8, 8, 8)

        // Add cells to row
        row.addView(trackingView)
        row.addView(descView)
        row.addView(senderView)
        row.addView(dateView)
        row.addView(qrView)

        // 👉 Click to open DocumentDetailsActivity
        row.setOnClickListener {
            val intent = Intent(this, DocumentDetailsActivity::class.java)
            intent.putExtra("documentId", documentId)
            startActivity(intent)
        }

        tableLayout.addView(row)
    }

    private fun makeCell(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 13f
        tv.setPadding(8, 8, 8, 8)
        tv.gravity = Gravity.CENTER
        return tv
    }

    private fun generateQrCode(data: String): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 100, 100)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    }
}
