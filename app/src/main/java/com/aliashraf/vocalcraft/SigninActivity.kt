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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignInActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference // Firebase Database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.signInButton)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference // Initialize the database reference

        signInButton.setOnClickListener {
            signInUser()
        }
    }

    private fun signInUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Email is required"
            return
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Password is required"
            return
        }

        // Sign in with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    if (user != null) {
                        retrieveUsername(user.email ?: "") // Retrieve the username by email
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Wrong Password or Email provided", Toast.LENGTH_SHORT).show()
                    Log.d("SignInActivity", "Sign in failed: ${task.exception?.message}")
                }
            }
    }

    private fun retrieveUsername(email: String) {
        // Query the Users node to find the username by email
        database.child("Users").orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val username = userSnapshot.child("name").getValue(String::class.java)
                            Log.d("SignInActivity", "Username retrieved: $username")

                            // Navigate to the PromptActivity with the username
                            val intent = Intent(this@SignInActivity, PromptActivity::class.java)
                            intent.putExtra("USERNAME", username) // Pass the username
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.d("SignInActivity", "No user found with the provided email")
                        Toast.makeText(this@SignInActivity, "No user found with the provided email", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("SignInActivity", "loadPost:onCancelled", databaseError.toException())
                }
            })
    }
}
