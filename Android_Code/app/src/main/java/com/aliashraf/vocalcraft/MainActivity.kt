package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Display the logo (assumed to be an ImageView in your layout)
        val logoImageView = findViewById<ImageView>(R.id.logoImageView2)

        // Use a Handler to delay the transition to the SignInMenuActivity
        Handler().postDelayed({
            // Start SignInMenuActivity
            val intent = Intent(this, SigninMenuActivity::class.java)
            startActivity(intent)
            finish() // Finish MainActivity so the user cannot go back to it
        }, 1000) // Delay of 1 second (1000 milliseconds)
    }
}
