package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONArray
import org.json.JSONObject

class NERActivity : AppCompatActivity() {

    private lateinit var dynamicFormContainer: LinearLayout
    private lateinit var submitButton: Button
    private val editTexts = mutableListOf<EditText>() // To hold references to EditTexts
    private var emptyCounter = 0
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.llm_form)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("prompts")

        // Find the container layout and submit button in the XML
        dynamicFormContainer = findViewById(R.id.dynamic_form_container)
        submitButton = findViewById(R.id.submitButton)

        // Get the JSON string passed through the Intent
        val jsonString = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")
        Log.d("MainActivity", "Prompt data: $promptData")

        // Check if the JSON string is not null or empty
        if (!jsonString.isNullOrEmpty()) {
            val jsonObject = JSONObject(jsonString)

            // Recursively generate views from the JSON object
            generateViewsFromJson(jsonObject, dynamicFormContainer)

            // Set up the submit button click listener
            submitButton.setOnClickListener {
                handleSubmit(jsonObject, promptData)
            }
        } else {
            // Handle the case where no JSON data was passed
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
        }
    }

    // Recursive function to generate views from JSON
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun generateViewsFromJson(jsonElement: Any, parentView: LinearLayout) {
        when (jsonElement) {
            is JSONObject -> {
                val iterator = jsonElement.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = jsonElement.get(key)

                    // Replace underscores with spaces in the key
                    val formattedKey = key.replace("_", " ")

                    if (value is JSONObject || value is JSONArray) {
                        // Create a TextView for the parent (if it's an object or array)
                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey // Use formatted key
                        textView.textSize = 18f
                        textView.setPadding(0, 16, 0, 8)
                        parentView.addView(textView)

                        // Create a new nested LinearLayout to hold child elements
                        val nestedLayout = LinearLayout(this)
                        nestedLayout.orientation = LinearLayout.VERTICAL
                        parentView.addView(nestedLayout)

                        // Recursively handle child elements
                        generateViewsFromJson(value, nestedLayout)
                    } else {
                        // Create a TextView for the key (parent node)
                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey // Use formatted key
                        textView.textSize = 16f
                        parentView.addView(textView)

                        // Create an EditText for the value (leaf node)
                        val editText = EditText(this)
                        editText.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        editText.setPadding(0, 0, 0, 16)
                        editText.hint = formattedKey
                        editText.setTextColor(ContextCompat.getColor(this, R.color.white)) // Set text color
                        editText.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray)) // Set hint text color
                        editText.background = resources.getDrawable(R.drawable.edit_text_boundary) // Set background
                        editText.setPadding(24, 12, 12, 12) // Set padding

                        // Add to the list of EditTexts
                        editTexts.add(editText)

                        // Set existing value in the EditText if present, else set empty
                        if (value is String && value.isNotEmpty()) {
                            editText.setText(value)
                        } else if (value is Int) {
                            editText.setText(value.toString())
                        } else if (value is Double) {
                            editText.setText(value.toString())
                        } else {
                            // Assign an ID to the EditText if the value is null or empty
                            val emptyId = emptyCounter // Numeric ID
                            editText.id = emptyId // Assign the ID
                            emptyCounter++ // Increment the empty counter
                        }

                        parentView.addView(editText)
                    }
                }
            }
            is JSONArray -> {
                // Handle array elements
                for (i in 0 until jsonElement.length()) {
                    val item = jsonElement.get(i)

                    // Create a TextView for array index or label
                    val textView = TextView(this)
                    textView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    textView.textSize = 16f
                    textView.setPadding(0, 16, 0, 8)
                    parentView.addView(textView)

                    // Recursively process each element in the array
                    generateViewsFromJson(item, parentView)
                }
            }
        }
    }

    // Handle submit button click
    private fun handleSubmit(jsonObject: JSONObject, promptData: String?) {
        var firstEmptyEditText: EditText? = null
        var allFilled = true

        // Convert JSONObject to String
        var jsonString = jsonObject.toString()

        // Count occurrences of "null"
        val nullCount = jsonString.split("null").size - 1 // Counts the number of "null"
        var replacementIndex = 0 // Track which EditText value to use for replacement

        for (i in 0 until emptyCounter) {
            val editText = findViewById<EditText>(i)

            if (replacementIndex < nullCount) {
                // Only replace if the new value is not empty
                if (editText.text.isNotEmpty()) {
                    // Replace only the next "null" with the current EditText value
                    jsonString = jsonString.replaceFirst("null", editText.text.toString())
                    replacementIndex++ // Move to the next replacement
                }
            }
        }

        if (replacementIndex < nullCount) {
            // If not all fields are filled, update the first empty EditText
            allFilled = false
            firstEmptyEditText = editTexts.firstOrNull { it.text.isEmpty() }
        }

        if (!allFilled) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            firstEmptyEditText?.requestFocus()
            scrollToView(firstEmptyEditText)
        } else {
            // All fields are filled; create the updated JSON string
            Log.d("MainActivity", "Received response from poster API: $jsonString")

            // Save to Firebase
            savePromptToFirebase(promptData, jsonString)

            // Show the JSON in a Toast or proceed with submission logic
            Toast.makeText(this, "Form Submitted. JSON: $jsonString", Toast.LENGTH_LONG).show()
        }
    }

    // Save the prompt to Firebase
    private fun savePromptToFirebase(promptData: String?, jsonString: String) {
        if (promptData != null) {
            // Query to check if a prompt with the same promptText exists
            database.orderByChild("promptText").equalTo(promptData).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // If it exists, get the existing prompt key
                        val existingPromptKey = snapshot.children.first().key
                        // Update the nerText for the existing prompt
                        existingPromptKey?.let {
                            database.child(it).child("nerText").setValue(jsonString)
                                .addOnSuccessListener {
                                    Log.d("MainActivity", "NerText updated successfully.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MainActivity", "Failed to update NerText: ${e.message}")
                                }
                        }
                    } else {
                        // If it doesn't exist, create a new prompt
                        val prompt = Prompt(promptText = promptData, nerText = jsonString)
                        database.push().setValue(prompt)
                            .addOnSuccessListener {
                                Log.d("MainActivity", "New prompt saved successfully.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainActivity", "Failed to save new prompt: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error querying database: ${e.message}")
                }
        } else {
            Log.e("MainActivity", "Prompt data is null.")
        }
    }


    // Scroll to the specified view
    private fun scrollToView(view: EditText?) {
        view?.let {
            val scrollView = findViewById<ScrollView>(R.id.scroll_view) // Get the ScrollView
            val y = it.y.toInt() + it.height // Get the y position of the EditText, plus its height
            scrollView.smoothScrollTo(0, y + 80) // Smooth scroll to the position
        }
    }
}
