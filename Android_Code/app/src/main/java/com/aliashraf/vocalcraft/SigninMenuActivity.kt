package com.aliashraf.vocalcraft

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils

class SigninMenuActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private val RC_SIGN_IN = 9001
    private lateinit var handler: Handler
    private lateinit var textHandler: Handler
    private lateinit var animationRunnable: Runnable
    private lateinit var textAnimationRunnable: Runnable
    private var currentTextIndex = 0
    private lateinit var floatingAnimator: ObjectAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin_menu)

        // Initialize handlers first
        handler = Handler(Looper.getMainLooper())
        textHandler = Handler(Looper.getMainLooper())

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInButton = findViewById<Button>(R.id.googleSignInButton)

        val signInButton1 = findViewById<Button>(R.id.signInButton)
        signInButton1.setOnClickListener(){
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val signupButton = findViewById<Button>(R.id.signUpButton)
        signupButton.setOnClickListener(){
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }


        // Set up the Google Sign-In button click listener
        signInButton.setOnClickListener {
            signIn()
        }

        val triggerSignIn = intent.getBooleanExtra("TRIGGER_SIGN_IN", false)
        if (triggerSignIn) {
            signIn()
        }

        setupFloatingAnimation()
        setupMicrophoneAnimation()
        setupTextAnimation()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("SigninMenuActivity", "Google sign in failed", e)
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Sign in with Google credentials
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("SigninMenuActivity", "firebaseAuthWithGoogle:" + account.id)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    Log.d("SigninMenuActivity", "signInWithCredential:success: ${user?.displayName}")
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    // Optionally, navigate to the main activity or another screen
                    val intent = Intent(this, ImageInsertActivity::class.java).apply {
                        putExtra("USERNAME", user?.displayName) // Pass the display name to the next activity
                    }
                    startActivity(intent)
                } else {
                    Log.w("SigninMenuActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // Set up floating animation for the microphone icon
    private fun setupFloatingAnimation() {
        val microphoneContainer = findViewById<CardView>(R.id.microphoneContainer)
        
        floatingAnimator = ObjectAnimator.ofFloat(
            microphoneContainer,
            View.TRANSLATION_Y,
            -20f,
            20f
        ).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupMicrophoneAnimation() {
        val microphoneIcon = findViewById<ImageView>(R.id.microphoneIcon)
        
        animationRunnable = object : Runnable {
            override fun run() {
                // First transition: Microphone to Arrow
                val fadeOut = ObjectAnimator.ofFloat(microphoneIcon, View.ALPHA, 1f, 0f)
                fadeOut.duration = 150
                
                val fadeIn = ObjectAnimator.ofFloat(microphoneIcon, View.ALPHA, 0f, 1f)
                fadeIn.duration = 150
                
                fadeOut.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        microphoneIcon.setImageResource(android.R.drawable.ic_media_play)
                        fadeIn.start()
                    }
                })
                
                fadeOut.start()

                // Second transition: Arrow back to Microphone after 1 second
                handler.postDelayed({
                    val fadeOutArrow = ObjectAnimator.ofFloat(microphoneIcon, View.ALPHA, 1f, 0f)
                    fadeOutArrow.duration = 150
                    
                    val fadeInMic = ObjectAnimator.ofFloat(microphoneIcon, View.ALPHA, 0f, 1f)
                    fadeInMic.duration = 150
                    
                    fadeOutArrow.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            microphoneIcon.setImageResource(android.R.drawable.ic_btn_speak_now)
                            fadeInMic.start()
                        }
                    })
                    
                    fadeOutArrow.start()
                }, 1000)

                // Schedule the next animation cycle
                handler.postDelayed(this, 3000)
            }
        }

        // Start the continuous animation
        handler.post(animationRunnable)
    }

    private fun setupTextAnimation() {
        val featureText = findViewById<TextView>(R.id.featureText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        
        val featureTexts = resources.getStringArray(R.array.feature_texts)
        val descriptionTexts = resources.getStringArray(R.array.description_texts)
        
        textAnimationRunnable = object : Runnable {
            override fun run() {
                // Fade out current texts
                val fadeOutFeature = ObjectAnimator.ofFloat(featureText, View.ALPHA, 1f, 0f)
                val fadeOutDesc = ObjectAnimator.ofFloat(descriptionText, View.ALPHA, 1f, 0f)
                
                // Slide up animation for current texts
                val slideUpFeature = ObjectAnimator.ofFloat(featureText, View.TRANSLATION_Y, 0f, -50f)
                val slideUpDesc = ObjectAnimator.ofFloat(descriptionText, View.TRANSLATION_Y, 0f, -50f)
                
                // Prepare next texts
                currentTextIndex = (currentTextIndex + 1) % featureTexts.size
                
                fadeOutFeature.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Update texts
                        featureText.text = featureTexts[currentTextIndex]
                        descriptionText.text = descriptionTexts[currentTextIndex]
                        
                        // Reset position for slide in
                        featureText.translationY = 50f
                        descriptionText.translationY = 50f
                        
                        // Fade in new texts
                        val fadeInFeature = ObjectAnimator.ofFloat(featureText, View.ALPHA, 0f, 1f)
                        val fadeInDesc = ObjectAnimator.ofFloat(descriptionText, View.ALPHA, 0f, 1f)
                        
                        // Slide up animation for new texts
                        val slideInFeature = ObjectAnimator.ofFloat(featureText, View.TRANSLATION_Y, 50f, 0f)
                        val slideInDesc = ObjectAnimator.ofFloat(descriptionText, View.TRANSLATION_Y, 50f, 0f)
                        
                        // Play fade in and slide in together
                        val animSetIn = AnimatorSet()
                        animSetIn.playTogether(fadeInFeature, fadeInDesc, slideInFeature, slideInDesc)
                        animSetIn.duration = 500
                        animSetIn.interpolator = AccelerateDecelerateInterpolator()
                        animSetIn.start()
                    }
                })
                
                // Play fade out and slide up together
                val animSetOut = AnimatorSet()
                animSetOut.playTogether(fadeOutFeature, fadeOutDesc, slideUpFeature, slideUpDesc)
                animSetOut.duration = 500
                animSetOut.interpolator = AccelerateDecelerateInterpolator()
                animSetOut.start()
                
                // Schedule next animation
                textHandler.postDelayed(this, 5000)
            }
        }
        
        // Start the text animation cycle
        textHandler.post(textAnimationRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove callbacks and cancel animations
        handler.removeCallbacks(animationRunnable)
        textHandler.removeCallbacks(textAnimationRunnable)
        floatingAnimator.cancel()
    }
}
