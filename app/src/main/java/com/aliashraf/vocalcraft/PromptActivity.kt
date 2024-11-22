package com.aliashraf.vocalcraft

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import java.io.File

// Data class to hold the prompt and its NER text
data class Prompt(
    var promptText: String = "", // Default value for promptText
    var nerText: String = "" // Default value for nerText
) {
    // No-argument constructor is implicitly provided by default values
}

class PromptActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference // Reference to Firebase Database
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var audioFile: File
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var promptButton: Button
    private lateinit var templatebutton: Button
    private lateinit var profileButton: Button
    private lateinit var ImageButton: Button


    private var isRecording = false
    private lateinit var promptsAdapter: PromptsAdapter
    private val promptsList = mutableListOf<Prompt>() // List to hold transcriptions
    private lateinit var recyclerView: RecyclerView
    private lateinit var editText: EditText // Declare editText at the class level

    private val client = OkHttpClient.Builder()
        .connectTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(100, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        database = Firebase.database.reference // Initialize the database reference

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view_prompts) // Make sure this ID matches your layout
        recyclerView.layoutManager = LinearLayoutManager(this)
        promptsAdapter = PromptsAdapter(promptsList) { promptText ->
            editText.setText(promptText) // Set the prompt text to the EditText
            closeDrawer() // Close the drawer
        }
        recyclerView.adapter = promptsAdapter

        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)

        val fabMicrophone = findViewById<FloatingActionButton>(R.id.fab_microphone)
        editText = findViewById(R.id.editText) // Initialize editText here
        val submitButton = findViewById<Button>(R.id.submitButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        promptButton = findViewById(R.id.promptbutton)
        ImageButton = findViewById(R.id.ImageButton)
        drawerLayout = findViewById(R.id.drawer_layout)
        promptButton.setOnClickListener {
            openDrawer()
        }
        templatebutton = findViewById(R.id.templateButton)
        templatebutton.setOnClickListener {
            val intent = Intent(this, TemplateActivity::class.java)
            startActivity(intent)
        }
        val username = intent.getStringExtra("USERNAME")
        if (!username.isNullOrEmpty()) {
            // set username in googleSignInButton
            googleSignInButton.text = "Welcome, $username"
        }

        submitButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#21bf63"))
        requestMicrophonePermission()

        fabMicrophone.setOnClickListener {
            if (isRecording) {
                stopRecording()
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Audio file path: ${audioFile.absolutePath}")
                sendAudioForTranscription(editText)
            } else {
                startRecording()
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            }
            isRecording = !isRecording
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        ImageButton.setOnClickListener {
            val intent = Intent(this, ImageActivity::class.java)
            startActivity(intent)
        }

        submitButton.setOnClickListener {
            val inputText = editText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                Log.d("MainActivity", "Submit clicked with prompt: $inputText")
                // Add prompt to Firebase Database with NER text as empty initially
                addPromptToDatabase(inputText, "") // NER text can be set after transcription
                callPosterGenerationApi(inputText) // Call the API to generate a poster
                editText.text.clear() // Clear the EditText after submission
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }

        // Load existing prompts from Firebase
        loadPromptsFromDatabase()
    }
    private fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START) // Close the drawer
        }
    }
    private fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START) // Open the drawer from the start
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

    private fun startRecording() {
        audioFile = File.createTempFile("audio", ".3gp", cacheDir)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        Log.d("MainActivity", "Started recording audio. Saving to: ${audioFile.absolutePath}")
    }

    private fun stopRecording() {
        mediaRecorder.apply {
            stop()
            release()
        }
        Log.d("MainActivity", "Stopped recording. Audio file saved at: ${audioFile.absolutePath}")
    }

    private fun sendAudioForTranscription(editText: EditText) {
        Log.d("MainActivity", "Preparing to send audio file: ${audioFile.name}")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, RequestBody.create("audio/3gp".toMediaTypeOrNull(), audioFile))
            .build()

        val request = Request.Builder()
            .url("https://pro-genuine-rooster.ngrok-free.app/transcribe")
            .post(requestBody)
            .build()

        Log.d("MainActivity", "Sending audio file to server...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to send audio: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PromptActivity, "Failed to send audio", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    Log.d("MainActivity", "Received response from server: $responseString")

                    runOnUiThread {
                        // Assuming the response is in the form: { "transcription": "What about my mother?", "ner": "NER Text" }
                        val (transcription, nerText) = parseTranscription(responseString)
                        promptsList.lastOrNull()?.let { lastPrompt ->
                            // Update the last prompt with its NER text
                            val updatedPrompt = lastPrompt.copy(nerText = nerText)
                            promptsList[promptsList.size - 1] = updatedPrompt // Update the prompt in the list
                            promptsAdapter.notifyItemChanged(promptsList.size - 1) // Notify adapter of updated item
                        }
                        editText.setText(transcription) // Set transcription to EditText
                    }
                } ?: Log.e("MainActivity", "Response body is null")
            }
        })
    }

    private fun parseTranscription(response: String): Pair<String, String> {
        // Parse the response JSON to extract the "transcription" and "ner" fields
        val transcription = Regex("\"transcription\"\\s*:\\s*\"(.*?)\"").find(response)?.groupValues?.get(1) ?: "Error"
        val nerText = Regex("\"ner\"\\s*:\\s*\"(.*?)\"").find(response)?.groupValues?.get(1) ?: ""
        return Pair(transcription, nerText)
    }

    private fun addPromptToDatabase(promptText: String, nerText: String) {
        // Check if the prompt already exists in the list
        val existingPrompt = promptsList.find { it.promptText == promptText }

        if (existingPrompt != null) {
            // If the prompt already exists, show a message and don't add it again
            Toast.makeText(this, "Prompt already exists", Toast.LENGTH_SHORT).show()
            return // Exit the method
        }

        // If the prompt does not exist, proceed to add it
        val promptId = database.push().key // Generate a unique key for each prompt
        promptId?.let {
            database.child("prompts").child(it).setValue(Prompt(promptText, nerText))
            promptsList.add(Prompt(promptText, nerText)) // Add to local list
            promptsAdapter.notifyItemInserted(promptsList.size - 1) // Notify adapter of new item
        }
    }
    private fun callPosterGenerationApi(promptText: String) {
        // Encode the prompt text to make it URL-safe
        val encodedPrompt = java.net.URLEncoder.encode(promptText, "UTF-8")

        // Construct the URL with the query parameter
        val urlWithQuery = "https://pro-genuine-rooster.ngrok-free.app/generate_poster/?user_input=$encodedPrompt"

        // Create a POST request with an empty body
        val requestBody = RequestBody.create(null, ByteArray(0)) // Empty request body

        // Create the request with the POST method
        val request = Request.Builder()
            .url(urlWithQuery)
            .post(requestBody) // Send an empty POST request with the query parameter in the URL
            .build()

        Log.d("MainActivity", "Sending prompt to generate poster API: $urlWithQuery")
        Toast.makeText(this, "Generating NER...", Toast.LENGTH_SHORT).show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to send request: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PromptActivity, "Failed to generate poster", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    Log.d("MainActivity", "Received response from poster API: $responseString")

                    // Handle the response as needed...

                    val intent = Intent(this@PromptActivity, NERActivity::class.java)
                    intent.putExtra("json_data", responseString) // Pass the response to NERActivity
                    intent.putExtra("prompt_data", promptText) // Pass the prompt text to NERActivity
                    startActivity(intent) // Start NERActivity


                } ?: Log.e("MainActivity", "Response body is null")
            }
        })
    }



    private fun loadPromptsFromDatabase() {
        database.child("prompts").get().addOnSuccessListener { dataSnapshot ->
            for (snapshot in dataSnapshot.children) {
                val prompt = snapshot.getValue(Prompt::class.java)
                prompt?.let {
                    promptsList.add(it) // Add to local list
                    promptsAdapter.notifyItemInserted(promptsList.size - 1) // Notify adapter of new item
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Error loading prompts: ${exception.message}")
        }
    }
}
