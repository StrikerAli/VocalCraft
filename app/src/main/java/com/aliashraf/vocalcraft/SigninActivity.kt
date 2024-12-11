package com.aliashraf.vocalcraft

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
    private lateinit var signupText: TextView
    private lateinit var googleSignInButton: Button
    private lateinit var signInButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference // Firebase Database reference

    private val allowedDomains = listOf(
        "gmail.com", "yahoo.com", "hotmail.com", "aol.com", "hotmail.co.uk", "hotmail.fr", "msn.com",
        "yahoo.fr", "wanadoo.fr", "orange.fr", "comcast.net", "yahoo.co.uk", "yahoo.com.br",
        "yahoo.co.in", "live.com", "rediffmail.com", "free.fr", "gmx.de", "web.de", "yandex.ru",
        "ymail.com", "libero.it", "outlook.com", "uol.com.br", "bol.com.br", "mail.ru", "cox.net",
        "hotmail.it", "sbcglobal.net", "sfr.fr", "live.fr", "verizon.net", "live.co.uk", "googlemail.com",
        "yahoo.es", "ig.com.br", "live.nl", "bigpond.com", "terra.com.br", "yahoo.it", "neuf.fr",
        "yahoo.de", "alice.it", "rocketmail.com", "att.net", "laposte.net", "facebook.com", "bellsouth.net",
        "yahoo.in", "hotmail.es", "charter.net", "yahoo.ca", "yahoo.com.au", "rambler.ru", "hotmail.de",
        "tiscali.it", "shaw.ca", "yahoo.co.jp", "sky.com", "earthlink.net", "optonline.net", "freenet.de",
        "t-online.de", "aliceadsl.fr", "virgilio.it", "home.nl", "qq.com", "telenet.be", "me.com",
        "yahoo.com.ar", "tiscali.co.uk", "yahoo.com.mx", "voila.fr", "gmx.net", "mail.com", "planet.nl",
        "tin.it", "live.it", "ntlworld.com", "arcor.de", "yahoo.co.id", "frontiernet.net", "hetnet.nl",
        "live.com.au", "yahoo.com.sg", "zonnet.nl", "club-internet.fr", "juno.com", "optusnet.com.au",
        "blueyonder.co.uk", "bluewin.ch", "skynet.be", "sympatico.ca", "windstream.net", "mac.com",
        "centurytel.net", "chello.nl", "live.ca", "aim.com", "bigpond.net.au", "nu.edu.pk"
    )

    // Initialize the Firebase Auth and Database
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signInButton = findViewById(R.id.loginButton)
        emailEditText = findViewById(R.id.emailEditText)
        signupText = findViewById(R.id.signupText)
        googleSignInButton = findViewById(R.id.googleSignInButton)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference // Initialize the database reference

        signupText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Google Sign in Click Watcher
        googleSignInButton.setOnClickListener {
            val intent = Intent(this, SigninMenuActivity::class.java)

            // Pass data in the intent
            intent.putExtra("TRIGGER_SIGN_IN", true) // Use a specific value like true
            startActivity(intent)
        }

        signInButton.setOnClickListener {
            signInUser()
        }
    }

    // Sign in the user with email and password
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

        // Check if the email domain is allowed
        if (!isValidDomain(email)) {
            Toast.makeText(this, "Email domain is not allowed", Toast.LENGTH_SHORT).show()
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

    // Check if the email domain is in the allowed domains list
    private fun isValidDomain(email: String): Boolean {
        val domain = email.substringAfter('@')
        return allowedDomains.contains(domain)
    }

    // Retrieve the username by email
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

                // Handle the error
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("SignInActivity", "loadPost:onCancelled", databaseError.toException())
                }
            })
    }
}
