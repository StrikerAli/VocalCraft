package com.aliashraf.vocalcraft

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group

import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import java.io.File

data class Prompt(
    var promptText: String = "",
    var nerText: String = ""
)

class PromptActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var audioFile: File
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var imageView: ImageView
    private lateinit var promptButton: Button
    private lateinit var submitButton: Button
    private lateinit var textView1: TextView
    private lateinit var textView_2: TextView
    private lateinit var icon1: ImageView
    private lateinit var icon2: ImageView
    private lateinit var editText: EditText

    private var isRecording = false
    private lateinit var promptsAdapter: PromptsAdapter
    private val promptsList = mutableListOf<Prompt>()
    private lateinit var recyclerView: RecyclerView

    private val client = OkHttpClient.Builder()
        .connectTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.editText)
        val rootView = findViewById<View>(R.id.main) // Your root layout (ConstraintLayout or any parent)
        val viewsToMove = listOf(
            findViewById<TextView>(R.id.welcomeTextView),
            findViewById<ImageView>(R.id.imageView3),
            findViewById<Button>(R.id.promptbutton),
            findViewById<ImageView>(R.id.imageView2),
            findViewById<TextView>(R.id.textView2)
        )

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is OPEN
                viewsToMove.forEach { view ->
                    view.translationY = -200 * resources.displayMetrics.density // Move up 200dp
                }
            } else {
                // Keyboard is CLOSED
                viewsToMove.forEach { view ->
                    view.translationY = 0f // Reset position
                }
            }
        }





        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        recyclerView = findViewById(R.id.recycler_view_prompts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        promptsAdapter = PromptsAdapter(promptsList) { promptText ->
            editText.setText(promptText)
            closeDrawer()
        }
        recyclerView.adapter = promptsAdapter

        imageView = findViewById(R.id.imageView)
        submitButton = findViewById(R.id.submitButton)
        promptButton = findViewById(R.id.promptbutton)
        drawerLayout = findViewById(R.id.drawer_layout)
        // Find all elements in the layout
        val mainLayout: View = findViewById(R.id.main) // The main ConstraintLayout
        val drawerLayout: View = findViewById(R.id.drawer_layout) // The DrawerLayout
        val navigationView: View = findViewById(R.id.navigation_view) // The NavigationView

        // Create a fade-in animation
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 3000 // 3 seconds
        }

        // Start the animation on the main layout and navigation view
        mainLayout.startAnimation(fadeIn)
        drawerLayout.startAnimation(fadeIn)
        navigationView.startAnimation(fadeIn)
        val icon1 = findViewById<ImageView>(R.id.icon1)
        val icon2 = findViewById<ImageView>(R.id.icon2)
        val textView1 = findViewById<TextView>(R.id.textView1)
        val textView_2 = findViewById<TextView>(R.id.textView_2)

        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)

        // Check if the intent has an extra named "Username"
        val intentUsername = intent.getStringExtra("USERNAME")
        val username = if (!intentUsername.isNullOrEmpty()) {
            intentUsername
        } else {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.displayName ?: currentUser?.email ?: "User"
        }

        val welcomeMessage = "Welcome, $username, Let's Get Started"
        welcomeTextView.text = welcomeMessage


        // Handle icon clicks
        icon1.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        icon2.setOnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }
        textView1.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        textView_2.setOnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }

        // Handle submit button click
        submitButton.setOnClickListener {
            val inputText = editText.text.toString().trim()

            if (inputText.isEmpty()) {
                // Record audio and send to API when EditText is empty
                if (isRecording) {
                    stopRecording()
                    Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                    Log.d("PromptActivity", "Audio file path: ${audioFile.absolutePath}")
                    sendAudioForTranscription(editText)
                } else {
                    startRecording()
                    Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                }
                isRecording = !isRecording
            } else {
                // Add prompt to database and call poster generation API
                Log.d("PromptActivity", "Submit clicked with prompt: $inputText")
                addPromptToDatabase(inputText, "")
                callPosterGenerationApi(inputText)
                editText.text.clear()
            }
        }
        promptButton.setOnClickListener {
            openDrawer()
        }

        //change color of Sumbit Button
        submitButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8C00"))
        promptButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8C00"))





        // TextWatcher to handle dynamic icon changes
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    imageView.setImageResource(R.drawable.ic_microphone)
                    imageView.rotation = 0F
                } else {
                    imageView.setImageResource(R.drawable.ic_paper_plane)
                    imageView.rotation = -40F
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        if (editText.text.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_microphone)
            imageView.rotation = 0F
        } else {
            imageView.setImageResource(R.drawable.ic_paper_plane)
            imageView.rotation = -40F
        }

        requestMicrophonePermission()
        loadPromptsFromDatabase()
    }

    private fun startRecording() {
        audioFile = File.createTempFile("audio", ".m4a", cacheDir)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        Log.d("PromptActivity", "Started recording audio. Saving to: ${audioFile.absolutePath}")
    }

    private fun stopRecording() {
        mediaRecorder.apply {
            stop()
            release()
        }
        Log.d("PromptActivity", "Stopped recording. Audio file saved at: ${audioFile.absolutePath}")
    }
    private fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START) // Open the drawer from the start
    }

    private fun sendAudioForTranscription(editText: EditText) {
        Log.d("PromptActivity", "Preparing to send audio file: ${audioFile.name}")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                RequestBody.create("audio/m4a".toMediaTypeOrNull(), audioFile) // Updated MIME type
            )
            .build()

        val request = Request.Builder()
            .url("http://35.171.159.55:8000/transcribe")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PromptActivity", "Failed to send audio: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PromptActivity, "Failed to send audio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    Log.d("PromptActivity", "Received response from server: $responseString")

                    runOnUiThread {
                        val (transcription, nerText) = parseTranscription(responseString)
                        editText.setText(transcription)
                    }
                } ?: Log.e("PromptActivity", "Response body is null")
            }
        })
    }


    private fun parseTranscription(response: String): Pair<String, String> {
        val transcription = Regex("\"transcription\"\\s*:\\s*\"(.*?)\"").find(response)?.groupValues?.get(1) ?: "Error"

        // Extract the actual text from Transcription(text='...')
        val textMatch = Regex("Transcription\\(text='(.*?)'").find(transcription)?.groupValues?.get(1) ?: "Error"

        val nerText = ""  // No nerText in your response
        return Pair(textMatch, nerText)
    }


    private fun addPromptToDatabase(promptText: String, nerText: String) {
        val existingPrompt = promptsList.find { it.promptText == promptText }

        if (existingPrompt != null) {
            Toast.makeText(this, "Prompt already exists", Toast.LENGTH_SHORT).show()
            return
        }

        val promptId = database.push().key
        promptId?.let {
            database.child("prompts").child(it).setValue(Prompt(promptText, nerText))
            promptsList.add(Prompt(promptText, nerText))
            promptsAdapter.notifyItemInserted(promptsList.size - 1)
        }
    }

    private fun callPosterGenerationApi(promptText: String) {
        // Define the API URL for the POST request
        val url = "http://35.171.159.55:8000/ner"

        // Create the JSON body with the "prompt" key
        val jsonBody = """{ "prompt": "$promptText" }"""

        // Create the request body
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            jsonBody
        )

        // Build the POST request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        // Create OkHttpClient and execute the request
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PromptActivity", "Failed to send request: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PromptActivity, "Failed to generate poster", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle the successful response
                    response.body?.let { responseBody ->
                        val responseString = responseBody.string()
                        Log.d("PromptActivity", "Received response from poster API: $responseString")

                        // Pass the response data and prompt text to the next activity
                        val intent = Intent(this@PromptActivity, NERActivity::class.java)
                        intent.putExtra("json_data", responseString)
                        intent.putExtra("prompt_data", promptText)
                        startActivity(intent)
                    }
                } else {
                    // Handle failure case when response is not successful
                    Log.e("PromptActivity", "Request failed with status code: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@PromptActivity, "Error in generating poster", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun requestMicrophonePermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                }
            }
        requestPermissionLauncher.launch(RECORD_AUDIO)
    }

    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun loadPromptsFromDatabase() {
        database.child("prompts").get().addOnSuccessListener { dataSnapshot ->
            for (snapshot in dataSnapshot.children) {
                val prompt = snapshot.getValue(Prompt::class.java)
                prompt?.let {
                    promptsList.add(it)
                    promptsAdapter.notifyItemInserted(promptsList.size - 1)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("PromptActivity", "Error loading prompts: ${exception.message}")
        }
    }
}
