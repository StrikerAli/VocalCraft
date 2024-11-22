package com.aliashraf.vocalcraft

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signUpButton = findViewById(R.id.signUpButton)
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        signUpButton.setOnClickListener {
            signUpUser()
        }
    }

    private fun signUpUser() {
        val database = Firebase.database
        val myRef = database.getReference("message")
        Log.d("SignUpActivity", "Database reference: $myRef")
        myRef.setValue("Hello, World!")
        Log.d("SignUpActivity", "Database write successful")
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (TextUtils.isEmpty(name)) {
            nameEditText.error = "Name is required"
            return
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Email is required"
            return
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password is required"
            return
        }

        // Create a new user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, store name and email in Realtime Database
                    val userId = auth.currentUser?.uid
                    val databaseReference = FirebaseDatabase.getInstance().getReference("Users")

                    val user = User(userId, name, email)

                    userId?.let {
                        databaseReference.child(it).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Registration Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this, PromptActivity::class.java)
                                    intent.putExtra("USERNAME", name) // Pass the name as username
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Log detailed error message for database write failure
                                    Toast.makeText(
                                        this,
                                        "Failed to store user data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e(
                                        "SignUpActivity",
                                        "Database error: ${dbTask.exception?.message}"
                                    )
                                }
                            }
                    } ?: run {
                        Toast.makeText(this, "User ID is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // If sign up fails, display a message to the user.
                    Toast.makeText(
                        this,
                        "Registration Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SignUpActivity", "Sign up failed: ${task.exception?.message}")
                }
            }
    }
}

    data class User(val userId: String?, val name: String, val email: String)
