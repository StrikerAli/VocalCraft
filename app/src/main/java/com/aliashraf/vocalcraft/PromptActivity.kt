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
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

        editText = findViewById(R.id.editText)
        imageView = findViewById(R.id.imageView)
        submitButton = findViewById(R.id.submitButton)
        promptButton = findViewById(R.id.promptbutton)
        drawerLayout = findViewById(R.id.drawer_layout)
        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val username = currentUser?.displayName ?: currentUser?.email ?: "User"
        val welcomeMessage = "Welcome, $username, Let's Get Started"
        welcomeTextView.setText(welcomeMessage)

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
        submitButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#5654f7"))
        promptButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#5654f7"))


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
        audioFile = File.createTempFile("audio", ".3gp", cacheDir)
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
            .addFormDataPart("file", audioFile.name, RequestBody.create("audio/3gp".toMediaTypeOrNull(), audioFile))
            .build()

        val request = Request.Builder()
            .url("https://pro-genuine-rooster.ngrok-free.app/transcribe")
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
        val nerText = Regex("\"ner\"\\s*:\\s*\"(.*?)\"").find(response)?.groupValues?.get(1) ?: ""
        return Pair(transcription, nerText)
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
        val encodedPrompt = java.net.URLEncoder.encode(promptText, "UTF-8")
        val urlWithQuery = "https://pro-genuine-rooster.ngrok-free.app/generate_poster/?user_input=$encodedPrompt"

        val requestBody = RequestBody.create(null, ByteArray(0))
        val request = Request.Builder()
            .url(urlWithQuery)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PromptActivity", "Failed to send request: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PromptActivity, "Failed to generate poster", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    Log.d("PromptActivity", "Received response from poster API: $responseString")

                    val intent = Intent(this@PromptActivity, NERActivity::class.java)
                    intent.putExtra("json_data", responseString)
                    intent.putExtra("prompt_data", promptText)
                    startActivity(intent)
                } ?: Log.e("PromptActivity", "Response body is null")
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
