package com.example.documenttracker.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.documenttracker.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

data class AppUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user"
)

class ManageUsersActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var listViewUsers: ListView
    private lateinit var btnAddUser: Button
    private lateinit var txtEmpty: TextView
    private lateinit var progressBar: ProgressBar

    private val users = mutableListOf<AppUser>()
    private val displayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        listViewUsers = findViewById(R.id.listViewUsers)
        btnAddUser = findViewById(R.id.btnAddUser)
        txtEmpty = findViewById(R.id.txtEmpty)
        progressBar = findViewById(R.id.progressBar)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listViewUsers.adapter = adapter

        btnAddUser.setOnClickListener { showAddUserDialog() }

        loadUsers()

        listViewUsers.setOnItemClickListener { _, _, position, _ ->
            val user = users[position]
            showUserOptionsDialog(user)
        }
    }

    private fun loadUsers() {
        progressBar.visibility = android.view.View.VISIBLE

        db.collection("users").addSnapshotListener { snapshots, error ->
            progressBar.visibility = android.view.View.GONE
            if (error != null) {
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            users.clear()
            displayList.clear()

            if (snapshots == null || snapshots.isEmpty) {
                txtEmpty.visibility = android.view.View.VISIBLE
                return@addSnapshotListener
            }

            for (doc in snapshots.documents) {
                val user = AppUser(
                    userId = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "user"
                )
                users.add(user)
                displayList.add("${user.name} (${user.role})\n${user.email}")
            }

            txtEmpty.visibility = android.view.View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)
        val edtRole = dialogView.findViewById<EditText>(R.id.edtRole)

        AlertDialog.Builder(this)
            .setTitle("Add New User")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = edtName.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val role = edtRole.text.toString().trim().ifEmpty { "user" }

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val options = FirebaseOptions.Builder()
                    .setApiKey(getString(R.string.firebase_api_key))
                    .setApplicationId(getString(R.string.firebase_app_id))
                    .setProjectId(getString(R.string.firebase_project_id))
                    .setStorageBucket(getString(R.string.firebase_storage_bucket))
                    .setDatabaseUrl(getString(R.string.firebase_database_url))
                    .build()

                val secondaryApp = try {
                    FirebaseApp.initializeApp(this, options, "SecondaryApp")
                } catch (e: IllegalStateException) {
                    FirebaseApp.getInstance("SecondaryApp")
                }

                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)

                // ✅ Create user in Auth and Firestore
                secondaryAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: return@addOnSuccessListener

                        val userData = mapOf(
                            "name" to name,
                            "email" to email,
                            "role" to role
                        )

                        db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }

                        secondaryAuth.signOut()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Auth error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserOptionsDialog(user: AppUser) {
        val options = arrayOf("Edit", "Delete", "Cancel")

        AlertDialog.Builder(this)
            .setTitle(user.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> showEditUserDialog(user)
                    1 -> deleteUser(user)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showEditUserDialog(user: AppUser) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtRole = dialogView.findViewById<EditText>(R.id.edtRole)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtPassword)

        edtName.setText(user.name)
        edtEmail.setText(user.email)
        edtRole.setText(user.role)
        edtPassword.visibility = android.view.View.GONE

        AlertDialog.Builder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newEmail = edtEmail.text.toString().trim()
                val updatedData = mapOf(
                    "name" to edtName.text.toString().trim(),
                    "email" to newEmail,
                    "role" to edtRole.text.toString().trim()
                )

                db.collection("users").document(user.userId).update(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show()
                        // Optional: update email in Auth
                        updateAuthEmail(user.userId, newEmail)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(user: AppUser) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}? This will remove their login access.")
            .setPositiveButton("Delete") { _, _ ->
                val options = FirebaseOptions.Builder()
                    .setApiKey(getString(R.string.firebase_api_key))
                    .setApplicationId(getString(R.string.firebase_app_id))
                    .setProjectId(getString(R.string.firebase_project_id))
                    .setStorageBucket(getString(R.string.firebase_storage_bucket))
                    .setDatabaseUrl(getString(R.string.firebase_database_url))
                    .build()

                val secondaryApp = try {
                    FirebaseApp.initializeApp(this, options, "SecondaryApp")
                } catch (e: IllegalStateException) {
                    FirebaseApp.getInstance("SecondaryApp")
                }

                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)

                // ✅ Sign in as admin again to delete user from Auth
                secondaryAuth.signInWithEmailAndPassword(user.email, "TemporaryPasswordIfKnown")
                    .addOnSuccessListener {
                        val currentUser = secondaryAuth.currentUser
                        currentUser?.delete()?.addOnSuccessListener {
                            // Delete Firestore record
                            db.collection("users").document(user.userId).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Firestore delete error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }?.addOnFailureListener {
                            Toast.makeText(this, "Auth delete error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        // fallback: delete from Firestore even if Auth deletion fails
                        db.collection("users").document(user.userId).delete()
                        Toast.makeText(this, "Deleted from Firestore only. (Auth password unknown)", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAuthEmail(uid: String, newEmail: String) {
        val options = FirebaseOptions.Builder()
            .setApiKey(getString(R.string.firebase_api_key))
            .setApplicationId(getString(R.string.firebase_app_id))
            .setProjectId(getString(R.string.firebase_project_id))
            .setStorageBucket(getString(R.string.firebase_storage_bucket))
            .setDatabaseUrl(getString(R.string.firebase_database_url))
            .build()

        val secondaryApp = try {
            FirebaseApp.initializeApp(this, options, "SecondaryApp")
        } catch (e: IllegalStateException) {
            FirebaseApp.getInstance("SecondaryApp")
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)
        val currentUser: FirebaseUser? = secondaryAuth.currentUser
        currentUser?.updateEmail(newEmail)
    }
}
